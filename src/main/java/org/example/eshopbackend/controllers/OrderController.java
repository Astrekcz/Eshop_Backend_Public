package org.example.eshopbackend.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eshopbackend.dto.shipment.CreateOrderRequestDTO;
import org.example.eshopbackend.dto.shipment.OrderResponseDTO;
import org.example.eshopbackend.entity.OrderEntity;
import org.example.eshopbackend.mapper.OrderMapper;
import org.example.eshopbackend.service.email.EmailService;
import org.example.eshopbackend.service.OrderService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final EmailService emailService;

    @PostMapping("/OrderRequest")
    public OrderResponseDTO createOrder(
            @Valid @RequestBody CreateOrderRequestDTO dto,
            @RequestHeader(value = "X-Adulto-Dev", required = false) String devFlag
    ) {
        // DEV bypass pro Adulto (pokud FE neposlal UID, ale pošle hlavičku)
        if ((dto.getAdultoczUid() == null || dto.getAdultoczUid().isBlank())
                && devFlag != null && !devFlag.isBlank()) {
            dto.setAdultoczUid("DEV-MOCK-UID-" + devFlag.toUpperCase()); // "OK" / "NOK"
        }

        // 1) vytvoř objednávku
        OrderEntity saved = orderService.addOrder(dto);

        // 2) po uložení zkus poslat potvrzovací e-mail (když selže, neblokuje to odpověď)
        try {
            emailService.sendOrderConfirmation(saved);
        } catch (Exception e) {
            log.warn("Nepodařilo se odeslat potvrzovací e-mail pro objednávku {}: {}",
                    saved.getOrderNumber(), e.getMessage());
        }

        // 3) vrať DTO
        return orderMapper.toDto(saved);
    }

    // OrderController.java (doplnění)
    @PostMapping("/{orderId}/notify-handover")
    public void notifyHandover(
            @PathVariable Long orderId,
            @RequestParam(required = false) String trackingNumber,
            @RequestParam(required = false) String trackingUrl
    ) {
        // orderService.requireById(...) = hoď 404/IllegalState, pokud neexistuje
        OrderEntity order = orderService.getOrderById(orderId);
        emailService.sendShipmentHandedOver(order, trackingNumber, trackingUrl);
    }


}
