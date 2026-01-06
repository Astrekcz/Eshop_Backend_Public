package org.example.eshopbackend.shipping.ppl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eshopbackend.entity.shipping.ShipmentEntity;
import org.example.eshopbackend.entity.shipping.ShipmentStatus;
import org.example.eshopbackend.repository.ShipmentRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShipmentSyncJob {
    private final ShipmentRepository shipmentRepo;
    private final PplClient pplClient;

    @Scheduled(fixedDelay = 2 * 60 * 1000, initialDelay = 10 * 1000)
    @Transactional
    public void refreshAllTrackings() {
        var page = shipmentRepo.findByStatusNotIn(
                List.of(ShipmentStatus.DELIVERED, ShipmentStatus.CANCELLED, ShipmentStatus.ERROR),
                Pageable.unpaged()
        );

        List<ShipmentEntity> active = page.getContent();

        for (ShipmentEntity s : active) {
            try {
                if (s.getTrackingNumber() == null || s.getTrackingNumber().isBlank()) {
                    var orderNo = s.getOrder() != null ? s.getOrder().getOrderNumber() : null;
                    if (orderNo != null && !orderNo.isBlank()) {
                        var st = pplClient.getStatus(orderNo);
                        if (st != null && st.getTrackingNumber() != null && !st.getTrackingNumber().isBlank()) {
                            s.setTrackingNumber(st.getTrackingNumber());
                            s.setStatus(mapTrackingToShipment(st.getRawStatus()));
                            s.setStatusText(st.getDescription());
                            s.setUpdatedAt(Instant.now());
                            log.info("[PPL CRON] doplněn TN {} pro objednávku {}", st.getTrackingNumber(), orderNo);
                            continue;
                        }
                    }
                    s.setUpdatedAt(Instant.now());
                    continue;
                }

                var st = pplClient.getStatus(s.getTrackingNumber());
                if (st == null) {
                    s.setUpdatedAt(Instant.now());
                    continue;
                }

                s.setTrackingNumber(st.getTrackingNumber());
                s.setStatus(mapTrackingToShipment(st.getRawStatus()));
                s.setStatusText(st.getDescription());
                s.setUpdatedAt(Instant.now());

                log.debug("[PPL CRON] {} → {}", s.getTrackingNumber(), s.getStatusText());
            } catch (Exception ex) {
                log.warn("[PPL CRON] {} tracking failed: {}", s.getTrackingNumber(), ex.getMessage());
                s.setStatus(ShipmentStatus.ERROR);
                s.setStatusText("Cron tracking failed: " + ex.getMessage());
                s.setUpdatedAt(Instant.now());
            }
        }
    }

    private ShipmentStatus mapTrackingToShipment(String raw) {
        if (raw == null || raw.isBlank()) return ShipmentStatus.REQUESTED;
        String r = raw.toUpperCase();
        if (r.contains("DELIVERED") || r.contains("DORUČ") || r.contains("DORUC")) return ShipmentStatus.DELIVERED;
        if (r.contains("CANCEL")) return ShipmentStatus.CANCELLED;
        if (r.contains("HAND") && r.contains("OVER")) return ShipmentStatus.HANDED_OVER;
        if (r.contains("TRANSIT") || r.contains("ROUTE")) return ShipmentStatus.IN_TRANSIT;
        if (r.contains("LABEL") || r.contains("PRINT")) return ShipmentStatus.LABEL_READY;
        if (r.contains("REQUEST") || r.contains("ACCEPT") || r.contains("CREATED") || r.contains("PENDING")) return ShipmentStatus.REQUESTED;
        return ShipmentStatus.REQUESTED;
    }
}
