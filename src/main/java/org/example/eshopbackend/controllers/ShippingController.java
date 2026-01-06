package org.example.eshopbackend.controllers;

import lombok.RequiredArgsConstructor;
import org.example.eshopbackend.dto.shipment.CreateShipmentCommand;
import org.example.eshopbackend.dto.shipment.ShipmentDTO;
import org.example.eshopbackend.dto.shipment.PplTrackingStatus;
import org.example.eshopbackend.shipping.ppl.ShipmentService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipping/shipments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ShippingController {

    private final ShipmentService shipmentService;

    /** Vytvoř zásilku u PPL + ulož štítek */
    @PostMapping("/{orderNumber}")
    public ShipmentDTO create(@PathVariable String orderNumber,
                              @RequestBody CreateShipmentCommand cmd) {
        return shipmentService.createShipmentAndSaveLabel(orderNumber, cmd);
    }

    /** Detail zásilky přes shipmentId (zároveň refreshne stav) */
    @GetMapping("/{shipmentId}")
    public ShipmentDTO get(@PathVariable Long shipmentId) {
        return shipmentService.refreshTracking(shipmentId);
    }

    /** Stažení uloženého štítku */
    @GetMapping("/{shipmentId}/label")
    public ResponseEntity<byte[]> downloadLabel(@PathVariable Long shipmentId) {
        var label = shipmentService.loadLabelFile(shipmentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(label.filename()).build().toString())
                .contentType(MediaType.parseMediaType(label.mime()))
                .body(label.bytes());
    }

    /** Manuální refresh trackingu (vrátí aktualizované DTO) */
    @PostMapping("/{shipmentId}/refresh")
    public ShipmentDTO refresh(@PathVariable Long shipmentId) {
        return shipmentService.refreshTracking(shipmentId);
    }

    /** Najdi zásilku podle čísla objednávky (404, pokud neexistuje) */
    @GetMapping("/by-order/{orderNumber}")
    public ResponseEntity<ShipmentDTO> byOrder(@PathVariable String orderNumber) {
        var dto = shipmentService.findDtoByOrderNumber(orderNumber);
        return dto.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Zjisti stav podle tracking čísla (i když zásilka není v DB) */
    @GetMapping("/track/{trackingNumber}")
    public PplTrackingStatus track(@PathVariable String trackingNumber) {
        return shipmentService.getTrackingByNumber(trackingNumber);
    }
}
