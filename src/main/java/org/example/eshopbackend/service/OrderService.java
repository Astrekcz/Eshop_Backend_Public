package org.example.eshopbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eshopbackend.adulto.AdultoVerifier;
import org.example.eshopbackend.dto.shipment.CreateOrderRequestDTO;
import org.example.eshopbackend.dto.shipment.OrderItemRequestDTO;
import org.example.eshopbackend.dto.shipment.UpdateOrderDTO;
import org.example.eshopbackend.entity.*;
import org.example.eshopbackend.exception.NotFoundException;
import org.example.eshopbackend.exception.OrderCreationException;
import org.example.eshopbackend.exception.OrderNotFoundException;
import org.example.eshopbackend.mapper.OrderMapper;
import org.example.eshopbackend.repository.OrderRepository;
import org.example.eshopbackend.repository.ProductRepository;
import org.example.eshopbackend.service.email.EmailService;
import org.example.eshopbackend.util.PhoneUtil;
import org.example.eshopbackend.util.VsUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.RoundingMode;
import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final EmailService emailService;
    private final AdultoVerifier adultoVerifier;
    private final ProductRepository productRepository;

    private static final int REQUIRED_AGE = 18;

    private String generateOrderNumber() {
        String year = String.valueOf(java.time.Year.now().getValue()); // např. "2025"
        String prefix = year + "-";

        Optional<OrderEntity> last = orderRepository.findTopByOrderNumberStartingWithOrderByOrderIdDesc(prefix);

        int nextSeq = 1;
        if (last.isPresent()) {
            String lastNumber = last.get().getOrderNumber(); // např. 2025-00042
            String[] parts = lastNumber.split("-");
            if (parts.length == 2) {
                try {
                    nextSeq = Integer.parseInt(parts[1]) + 1;
                } catch (NumberFormatException ignored) {}
            }
        }

        return String.format("%s%05d", prefix, nextSeq); // např. 2025-00043
    }

    @Transactional(readOnly = true)
    public OrderEntity findByOrderNumberOrThrow(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderNumber));
    }

    private static long toKc(BigDecimal v) {
        return v.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    @Transactional
    public OrderEntity addOrder(CreateOrderRequestDTO req) {
        // 1) 18+ verifikace (Adulto nebo DEV mock)
        String uid = (req.getAdultoczUid() == null || req.getAdultoczUid().isBlank())
                ? resolveDevAdultoUidFromHeader()
                : req.getAdultoczUid();

        boolean isDevMode = uid != null && uid.startsWith("DEV-MOCK-UID-");

        if (isDevMode) {
            boolean isAdult = uid.endsWith("-OK");
            if (!isAdult) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Nákup je povolen osobám starším 18 let");
            }
            log.info("DEV mode: Age verification bypassed");
        } else {
            var ver = adultoVerifier.verify(uid, REQUIRED_AGE);
            if (!ver.adult()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Nákup je povolen osobám starším 18 let");
            }
        }

        // 2) Telefon – normalizace na E.164 (defenzivně i po DTO validaci)
        final String phoneE164 = PhoneUtil.toE164OrNull(req.getCustomerPhone(), "CZ");
        if (phoneE164 == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Zadejte platné telefonní číslo ve tvaru +420…");
        }

        // 3) Základ objednávky
        OrderEntity order = orderMapper.toEntity(req);
        order.setOrderStatus(OrderStatus.NEW);
        order.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        String orderNumber = generateOrderNumber();
        order.setOrderNumber(orderNumber);
        order.setBankVs(VsUtil.fromOrderNumber(orderNumber));
        order.setAgeConfirmed(true);

        // přepiš telefon v entitě na E.164
        // POZOR: pokud má entita jiný název pole, uprav na správný setter
        order.setCustomerPhone(phoneE164);

        BigDecimal computedSubtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        // Snapshot položek
        for (OrderItemRequestDTO it : req.getItems()) {
            Product p = productRepository.findById(it.getProductId())
                    .orElseThrow(() -> new NotFoundException("Produkt neexistuje: id=" + it.getProductId()));

            int qty = Math.max(1, it.getQuantity());
            BigDecimal unit = p.getPrice().setScale(2, RoundingMode.HALF_UP);
            BigDecimal line = unit.multiply(BigDecimal.valueOf(qty));

            OrderItemEntity item = OrderItemEntity.builder()
                    .order(order)
                    .nameOfProduct(p.getProductName())
                    .name(p.getSlug())
                    .amountOfProducts(qty)
                    .unitPriceCzk(unit)
                    .lineTotalCzk(line)
                    .weightGrams(p.getWeightGrams())
                    .build();

            order.getItems().add(item);
            computedSubtotal = computedSubtotal.add(line);
        }

        // 4) Doprava a total
        BigDecimal shipping = (req.getShippingCzk() != null)
                ? BigDecimal.valueOf(req.getShippingCzk()).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (shipping.signum() < 0) shipping = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal computedTotal = computedSubtotal.add(shipping);

        long subtotalKc = toKc(computedSubtotal);
        long shippingKc = toKc(shipping);
        long totalKc    = Math.addExact(subtotalKc, shippingKc);

        order.setSubtotalCzk(subtotalKc);
        order.setShippingCzk(shippingKc);
        order.setTotalCzk(totalKc);

        if (req.getTotalCzk() != null && !req.getTotalCzk().equals(totalKc)) {
            log.warn("Total z FE ({}) != BE ({})", req.getTotalCzk(), totalKc);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Součet na serveru nesouhlasí.");
        }

        try {
            OrderEntity saved = orderRepository.save(order);
            // Pozn.: potvrzovací e-mail posíláme zde.
            // Pokud ho současně posíláš i v controlleru, jednu z těch dvou cest vypni, ať to nejde dvakrát.
            emailService.sendOrderConfirmation(saved);
            return saved;
        } catch (Exception e) {
            log.error("Error while creating order", e);
            throw new OrderCreationException("Failed to create order", e);
        }
    }

    // --- pomocná metoda pro DEV režim ---
    private String resolveDevAdultoUidFromHeader() {
        try {
            var attrs = (org.springframework.web.context.request.ServletRequestAttributes)
                    org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            String devFlag = attrs.getRequest().getHeader("X-Adulto-Dev");
            if (devFlag == null || devFlag.isBlank()) return null;
            return "DEV-MOCK-UID-" + devFlag.toUpperCase(); // očekávej "OK" nebo "NOK"
        } catch (Exception ignored) {
            return null;
        }
    }

    // Read
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Page<OrderEntity> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    // Read (detail)
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public OrderEntity getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    // Update — po přechodu na SHIPPED pošli e-mail o předání dopravci
    //         + po přechodu na PAID pošli potvrzení platby
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public OrderEntity updateOrder(Long id, UpdateOrderDTO updateDTO) {
        OrderEntity orderEntity = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        // 1. Uložíme si starý status před změnou
        OrderStatus oldStatus = orderEntity.getOrderStatus();

        // 2. MapStruct aplikuje změny z DTO (včetně nového statusu, pokud tam je)
        orderMapper.updateOrder(orderEntity, updateDTO);

        // 3. Uložíme do DB
        OrderEntity saved = orderRepository.save(orderEntity);

        // 4. Zjistíme nový status
        OrderStatus newStatus = saved.getOrderStatus();

        // --- Logika pro odesílání e-mailů podle změny stavu ---

        // A) Pokud se změnilo na SHIPPED (a předtím to shipped nebylo)
        if (oldStatus != OrderStatus.SHIPPED && newStatus == OrderStatus.SHIPPED) {
            try {
                emailService.sendOrderShipped(saved);
            } catch (Exception e) {
                log.warn("Nepodařilo se odeslat e-mail o předání dopravci pro objednávku {}: {}",
                        saved.getOrderNumber(), e.getMessage());
            }
        }

        // B) NOVÉ: Pokud se změnilo na PAID (a předtím to paid nebylo)
        if (oldStatus != OrderStatus.PAID && newStatus == OrderStatus.PAID) {
            try {
                emailService.sendPaymentReceived(saved);
            } catch (Exception e) {
                log.warn("Nepodařilo se odeslat e-mail o přijetí platby pro objednávku {}: {}",
                        saved.getOrderNumber(), e.getMessage());
            }
        }

        return saved;
    }

    // Delete
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new OrderNotFoundException(id);
        }
        orderRepository.deleteById(id);
    }
}
