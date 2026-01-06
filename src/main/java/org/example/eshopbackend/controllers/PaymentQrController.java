// src/main/java/org/example/zeniqbackend/controllers/PaymentQrController.java
package org.example.eshopbackend.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eshopbackend.config.PaymentProps;
import org.example.eshopbackend.service.OrderService;
import org.example.eshopbackend.util.QrGenerator;
import org.example.eshopbackend.util.Spayd;
import org.example.eshopbackend.util.VsUtil;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentQrController {

    private final OrderService orderService;
    private final PaymentProps paymentProps;

    @GetMapping(value = "/qr/{orderNumber}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<?> qr(@PathVariable String orderNumber) {
        var order = orderService.findByOrderNumberOrThrow(orderNumber);

        // 1) IBAN normalizace
        String iban = paymentProps.getIban();
        if (iban == null) {
            return textError(HttpStatus.PRECONDITION_FAILED, "IBAN není nastaven (payments.bank.iban).");
        }
        iban = iban.replaceAll("\\s+", "").toUpperCase();
        if (!iban.matches("^[A-Z0-9]{15,34}$")) {
            log.warn("QR: nevalidní IBAN v konfiguraci: '{}'", iban);
            return textError(HttpStatus.PRECONDITION_FAILED, "Neplatný IBAN v konfiguraci (15–34 znaků A–Z/0–9, bez mezer).");
        }

        // 2) Částka – v DB máš Kč (integer) → převeď na haléře
        long kc = order.getTotalCzk() == null ? 0L : Math.max(0L, order.getTotalCzk());
        long amountHaler = kc * 100L;  // <<< Tohle musí jít do Spayd.build

        // 3) VS – validuj, případně spočti z orderNumber
        String vs = (order.getBankVs() != null && order.getBankVs().matches("^\\d{1,10}$"))
                ? order.getBankVs()
                : VsUtil.fromOrderNumber(order.getOrderNumber());

        // 4) Sestav SPD
        final String spd;
        try {
            spd = Spayd.build(
                    iban,
                    amountHaler, // <<< HALÉŘE
                    vs,
                    "Objednávka " + order.getOrderNumber(),
                    paymentProps.getBic()
            );
        } catch (IllegalArgumentException iae) {
            log.warn("QR: SPAYD build fail for order {}: {}", orderNumber, iae.getMessage());
            return textError(HttpStatus.PRECONDITION_FAILED, "SPAYD chyba: " + iae.getMessage());
        } catch (Exception e) {
            log.error("QR: Neočekávaná chyba při sestavení SPD pro {}", orderNumber, e);
            return textError(HttpStatus.INTERNAL_SERVER_ERROR, "Neočekávaná chyba při sestavení SPD.");
        }

        // 5) PNG z SPD
        try {
            byte[] png = QrGenerator.toPng(spd, 512);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(png);
        } catch (Exception e) {
            log.error("QR: Generování PNG selhalo pro {}", orderNumber, e);
            return textError(HttpStatus.INTERNAL_SERVER_ERROR, "Generování QR obrázku selhalo.");
        }
    }

    private ResponseEntity<String> textError(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.TEXT_PLAIN)
                .cacheControl(CacheControl.noStore())
                .body(message);
    }
}
