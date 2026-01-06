// src/main/java/org/example/zeniqbackend/shipping/ppl/ShipmentService.java
package org.example.eshopbackend.shipping.ppl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eshopbackend.dto.shipment.CreateShipmentCommand;
import org.example.eshopbackend.dto.shipment.PplCreateShipmentRequestDTO;
import org.example.eshopbackend.dto.shipment.PplCreateShipmentResultDTO;
import org.example.eshopbackend.dto.shipment.PplTrackingStatus;
import org.example.eshopbackend.dto.shipment.ShipmentDTO;
import org.example.eshopbackend.entity.OrderEntity;
import org.example.eshopbackend.entity.shipping.ShipmentEntity;
import org.example.eshopbackend.entity.shipping.ShipmentStatus;
import org.example.eshopbackend.mapper.ShipmentMapper;
import org.example.eshopbackend.repository.OrderRepository;
import org.example.eshopbackend.repository.ShipmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final OrderRepository orderRepo;
    private final ShipmentRepository shipmentRepo;
    private final ShipmentMapper mapper;
    private final PplClient pplClient;

    @Value("${shipping.labels.dir:./labels}")
    private String labelsDir;

    @Value("${shipping.packaging.weight-grams:30}")
    private int packagingWeightGrams;

    @Transactional
    public ShipmentDTO createShipmentAndSaveLabel(String orderNumber, CreateShipmentCommand cmd) {
        OrderEntity order = orderRepo.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNumber));

        var existing = shipmentRepo.findByOrder_OrderNumber(orderNumber);
        if (existing.isPresent()) {
            return mapper.toDto(existing.get());
        }

        // 1) namapuj objednávku + admin command na request pro PPL
        PplCreateShipmentRequestDTO req = mapper.toPplRequest(order, cmd, packagingWeightGrams);

        // 2) zavolej PPL – může vrátit label hned, nebo až později (labelPdf = null)
        PplCreateShipmentResultDTO pplResult = pplClient.createShipmentWithLabel(
                req,
                Math.max(1, cmd.getPiecesCount()),
                cmd.getLabelFormat(),
                cmd.getLabelDpi(),
                cmd.isCompleteLabelRequested(),
                cmd.getDepot()
        );

        // 3) ulož štítek na disk, pokud už dorazil
        Path file = null;
        String mime = pplResult.getLabelMime() == null ? "application/pdf" : pplResult.getLabelMime();
        if (pplResult.getLabelPdf() != null && pplResult.getLabelPdf().length > 0) {
            String ext = mime.toLowerCase().contains("png") ? "png" : "pdf";
            Path dir = Path.of(labelsDir);
            file = dir.resolve("shipment-" + orderNumber + "." + ext);
            try {
                Files.createDirectories(dir);
                Files.write(file, pplResult.getLabelPdf());
            } catch (Exception e) {
                log.warn("Ukládání štítku selhalo: {}", file, e);
                file = null; // spadni do „pending“, ale neházej výjimku
            }
        }

        // 4) persist ShipmentEntity
        ShipmentEntity entity = new ShipmentEntity();
        entity.setOrder(order);
        entity.setTrackingNumber(pplResult.getTrackingNumber()); // může být null
        entity.setPplBatchId(pplResult.getBatchId());
        entity.setPiecesCount(Math.max(1, cmd.getPiecesCount()));
        entity.setProductType(req.getServiceCode());
        entity.setDepot(cmd.getDepot());
        entity.setLabelPath(file != null ? file.toString() : null);
        entity.setLabelMime(file != null ? mime : null);

        if (file != null) {
            entity.setStatus(ShipmentStatus.LABEL_READY);
            entity.setStatusText("Label stored" + (pplResult.getTrackingNumber() == null ? " (tracking pending)" : ""));
        } else {
            entity.setStatus(ShipmentStatus.REQUESTED);
            entity.setStatusText("Label pending");
        }

        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        entity = shipmentRepo.save(entity);
        return mapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public LabelFile loadLabelFile(Long shipmentId) {
        ShipmentEntity entity = shipmentRepo.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found: " + shipmentId));

        if (entity.getLabelPath() == null) {
            throw new IllegalStateException("Shipment nemá uložený štítek");
        }
        try {
            Path p = Path.of(entity.getLabelPath());
            byte[] bytes = Files.readAllBytes(p);
            String mime = p.toString().toLowerCase().endsWith(".pdf") ? "application/pdf" : "image/png";
            return new LabelFile(bytes, mime, p.getFileName().toString());
        } catch (Exception e) {
            throw new RuntimeException("Chyba při čtení štítku: " + entity.getLabelPath(), e);
        }
    }

    @Transactional
    public ShipmentDTO refreshTracking(Long shipmentId) {
        ShipmentEntity entity = shipmentRepo.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found: " + shipmentId));

        if (entity.getTrackingNumber() == null || entity.getTrackingNumber().isBlank()) {
            // Nemáme zatím tracking – nepanikař; jen aktualizuj timestamp a vrať DTO
            entity.setUpdatedAt(Instant.now());
            shipmentRepo.save(entity);
            return mapper.toDto(entity);
        }

        try {
            PplTrackingStatus st = pplClient.getStatus(entity.getTrackingNumber());
            ShipmentStatus newStatus = mapTrackingToShipment(st.getRawStatus());
            entity.setStatus(newStatus);
            entity.setStatusText(st.getDescription());
            entity.setUpdatedAt(Instant.now());
            shipmentRepo.save(entity);
        } catch (Exception ex) {
            log.error("Tracking refresh failed for {}: {}", entity.getTrackingNumber(), ex.getMessage(), ex);
            entity.setStatus(ShipmentStatus.ERROR);
            entity.setStatusText("Tracking refresh failed: " + ex.getMessage());
            entity.setUpdatedAt(Instant.now());
            shipmentRepo.save(entity);
        }

        return mapper.toDto(entity);
    }

    /** Bezpečná varianta: neháže, když shipment neexistuje (např. NEW/nezaplacená objednávka). */
    @Transactional(readOnly = true)
    public Optional<ShipmentDTO> findDtoByOrderNumber(String orderNumber) {
        return shipmentRepo.findByOrder_OrderNumber(orderNumber)
                .map(mapper::toDto);
    }

    /** Původní API – nyní už NEHÁZÍ; pokud zásilka neexistuje, vrací null. */
    @Deprecated
    @Transactional(readOnly = true)
    public ShipmentDTO getByOrderNumber(String orderNumber) {
        return findDtoByOrderNumber(orderNumber).orElse(null);
    }

    @Transactional(readOnly = true)
    public ShipmentDTO getById(Long shipmentId) {
        ShipmentEntity entity = shipmentRepo.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found: " + shipmentId));
        return mapper.toDto(entity);
    }

    private ShipmentStatus mapTrackingToShipment(String raw) {
        if (raw == null || raw.isBlank()) return ShipmentStatus.REQUESTED;
        String r = raw.toUpperCase();
        if (r.contains("DELIVERED") || r.contains("DORUČ") || r.contains("DORUC")) return ShipmentStatus.DELIVERED;
        if (r.contains("CANCEL")) return ShipmentStatus.CANCELLED;
        if (r.contains("HAND") && r.contains("OVER")) return ShipmentStatus.HANDED_OVER;
        if (r.contains("IN_TRANSIT") || r.contains("TRANSIT") || r.contains("ROUTE")) return ShipmentStatus.IN_TRANSIT;
        if (r.contains("LABEL") || r.contains("PRINT")) return ShipmentStatus.LABEL_READY;
        if (r.contains("REQUEST") || r.contains("ACCEPT") || r.contains("CREATED") || r.contains("PENDING")) return ShipmentStatus.REQUESTED;
        return ShipmentStatus.REQUESTED;
    }

    public record LabelFile(byte[] bytes, String mime, String filename) {}

    @Transactional(readOnly = true)
    public PplTrackingStatus getTrackingByNumber(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new IllegalArgumentException("Tracking number is empty");
        }
        return pplClient.getStatus(trackingNumber);
    }
}
