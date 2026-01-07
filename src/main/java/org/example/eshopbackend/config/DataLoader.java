package org.example.eshopbackend.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eshopbackend.dto.shipment.CreateOrderRequestDTO;
import org.example.eshopbackend.entity.OrderEntity;
import org.example.eshopbackend.entity.OrderStatus;
import org.example.eshopbackend.entity.PaymentMethod;
import org.example.eshopbackend.entity.User;
import org.example.eshopbackend.entity.shipping.ShipmentEntity;
import org.example.eshopbackend.entity.shipping.ShipmentStatus;
import org.example.eshopbackend.mapper.OrderMapper;
import org.example.eshopbackend.repository.OrderRepository;
import org.example.eshopbackend.repository.ShipmentRepository;
import org.example.eshopbackend.repository.UserRepository;
import org.example.eshopbackend.util.VsUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;

    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final OrderMapper orderMapper;

    @Override
    public void run(String... args) throws Exception {
        loadUsers();
        loadOrders();
        applySeedShipments();
    }

    /* === USERS === */
    private void loadUsers() throws Exception {
        var res = new ClassPathResource("startup_data/users.json");
        if (!res.exists()) {
            log.info("No startup users (startup_data/users.json missing) – skipping.");
            return;
        }
        try (InputStream inputStream = res.getInputStream()) {
            List<User> users = objectMapper.readValue(inputStream, new TypeReference<List<User>>() {});
            if (users == null || users.isEmpty()) {
                log.info("startup_data/users.json empty – nothing to import.");
                return;
            }
            for (User user : users) {
                Optional<User> existingUser = userRepository.findByEmail(user.getEmail());
                if (existingUser.isEmpty()) {
                    if (user.getPassword() != null && !user.getPassword().isBlank()) {
                        user.setPassword(passwordEncoder.encode(user.getPassword()));
                    }
                    userRepository.save(user);
                }
            }
        }
    }

    /* === ORDERS === */
    private void loadOrders() throws Exception {
        var res = new ClassPathResource("startup_data/orders.json");
        if (!res.exists()) {
            log.info("No startup orders (startup_data/orders.json missing) – skipping.");
            return;
        }

        try (InputStream is = res.getInputStream()) {
            List<com.fasterxml.jackson.databind.JsonNode> nodes =
                    objectMapper.readValue(is, new com.fasterxml.jackson.core.type.TypeReference<>() {});

            if (nodes == null || nodes.isEmpty()) {
                log.info("startup_data/orders.json empty – nothing to import.");
                return;
            }

            for (var node : nodes) {
                // 1) mapni známá pole do existujícího DTO
                CreateOrderRequestDTO dto = objectMapper.treeToValue(node, CreateOrderRequestDTO.class);

                // 2) DTO -> Entity
                OrderEntity entity = orderMapper.toEntity(dto);

                // 3) orderStatus z JSONu (volitelný), default NEW
                String statusStr = node.hasNonNull("orderStatus")
                        ? node.get("orderStatus").asText()
                        : "NEW";
                entity.setOrderStatus(OrderStatus.valueOf(statusStr));

                // 4) povinné/implicitní hodnoty
                entity.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
                entity.setAgeConfirmed(true);

                // 5) vygeneruj číslo objednávky
                String orderNumber = generateOrderNumber();
                entity.setOrderNumber(orderNumber);

                // 6) VS z čísla objednávky
                entity.setBankVs(VsUtil.fromOrderNumber(orderNumber));

                // 7) hygiena
                if (entity.getShipCountryCode() == null || entity.getShipCountryCode().isBlank()) {
                    entity.setShipCountryCode("CZ");
                }
                if (entity.getShipPostalCode() != null) {
                    entity.setShipPostalCode(entity.getShipPostalCode().replaceAll("\\s+", ""));
                }

                // 8) konzistence součtů
                if (entity.getSubtotalCzk() != null && entity.getShippingCzk() != null) {
                    entity.setTotalCzk(entity.getSubtotalCzk() + entity.getShippingCzk());
                }

                orderRepository.save(entity);
                log.info("Seeded order {} (status {})", entity.getOrderNumber(), entity.getOrderStatus());
            }
        }
    }

    /* === SHIPMENTS === */
    private void applySeedShipments() throws Exception {
        var res = new ClassPathResource("startup_data/shipments.json");

        // ZMĚNA: Pokud soubor neexistuje, prostě končíme. Žádné automatické generování nesmyslů.
        if (!res.exists()) {
            return;
        }

        try (InputStream is = res.getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            if (root == null || !root.isArray() || root.size() == 0) {
                log.info("startup_data/shipments.json empty – skipping.");
                return;
            }
            for (JsonNode n : root) {
                String orderNumber = text(n, "orderNumber");
                String tracking    = text(n, "trackingNumber");
                String statusStr   = text(n, "status");

                if (isBlank(orderNumber) || isBlank(tracking) || isBlank(statusStr)) {
                    log.warn("Shipments seed row missing fields: {}", n);
                    continue;
                }

                var orderOpt = orderRepository.findByOrderNumber(orderNumber);
                if (orderOpt.isEmpty()) {
                    log.warn("Order {} not found for shipments seed", orderNumber);
                    continue;
                }
                var order = orderOpt.get();

                ShipmentEntity sh = shipmentRepository.findByOrder(order).orElseGet(() -> {
                    ShipmentEntity s = new ShipmentEntity();
                    s.setOrder(order);
                    return s;
                });

                ShipmentStatus sst = parseStatus(statusStr);
                sh.setTrackingNumber(tracking);
                sh.setStatus(sst);
                shipmentRepository.save(sh);

                // === mapování ShipmentStatus -> OrderStatus ===
                switch (sst) {
                    case REQUESTED, LABEL_READY, HANDED_OVER, IN_TRANSIT -> {
                        if (order.getOrderStatus() != OrderStatus.CANCELED) {
                            order.setOrderStatus(OrderStatus.SHIPPED);
                        }
                    }
                    case DELIVERED -> order.setOrderStatus(OrderStatus.DELIVERED);
                    case CANCELLED -> order.setOrderStatus(OrderStatus.CANCELED);
                    case NEW, ERROR -> {
                        log.info("Order {} shipment status {}, order status unchanged ({})",
                                orderNumber, sst, order.getOrderStatus());
                    }
                }

                orderRepository.save(order);
                log.info("Seeded shipment for order {} -> {} ({})",
                        orderNumber, tracking, sst);
            }
        }
    }

    /* === Helpers === */

    private String generateOrderNumber() {
        String year = String.valueOf(java.time.Year.now().getValue());
        String prefix = year + "-";
        Optional<OrderEntity> last =
                orderRepository.findTopByOrderNumberStartingWithOrderByOrderIdDesc(prefix);

        int nextSeq = 1;
        if (last.isPresent()) {
            String lastNumber = last.get().getOrderNumber();
            String[] parts = lastNumber.split("-");
            if (parts.length == 2) {
                try {
                    nextSeq = Integer.parseInt(parts[1]) + 1;
                } catch (NumberFormatException ignored) {}
            }
        }
        return String.format("%s%05d", prefix, nextSeq);
    }

    private ShipmentStatus parseStatus(String s) {
        try {
            return ShipmentStatus.valueOf(s.trim().toUpperCase());
        } catch (Exception e) {
            log.warn("Unknown shipment status '{}', falling back to REQUESTED", s);
            return ShipmentStatus.REQUESTED;
        }
    }

    private String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}