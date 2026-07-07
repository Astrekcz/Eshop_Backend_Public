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

import java.time.LocalDateTime; // Změněno z Instant na LocalDateTime
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
        // Načteme všechny nedoručené zásilky
        var page = shipmentRepo.findByStatusNotIn(
                List.of(ShipmentStatus.DELIVERED, ShipmentStatus.CANCELLED, ShipmentStatus.ERROR),
                Pageable.unpaged()
        );

        List<ShipmentEntity> active = page.getContent();

        for (ShipmentEntity s : active) {
            try {
                // Pokud zásilka nemá Tracking Number, přeskočíme ji.
                if (s.getTrackingNumber() == null || s.getTrackingNumber().isBlank()) {
                    continue;
                }

                // Bezpečně voláme PPL
                var st = pplClient.getStatus(s.getTrackingNumber());

                if (st == null) {
                    s.setUpdatedAt(LocalDateTime.now());
                    continue;
                }

                // Aktualizace stavu podle PPL
                s.setTrackingNumber(st.getTrackingNumber());
                s.setStatus(mapTrackingToShipment(st.getRawStatus()));
                s.setStatusText(st.getDescription());
                s.setUpdatedAt(LocalDateTime.now());

                log.debug("[PPL CRON] {} → {}", s.getTrackingNumber(), s.getStatusText());

            } catch (Exception ex) {
                log.warn("[PPL CRON] {} tracking failed: {}", s.getTrackingNumber(), ex.getMessage());
                // Pokud selže sync, jen aktualizujeme timestamp (BaseEntity by to zvládla taky, ale pro sichr v paměti)
                s.setUpdatedAt(LocalDateTime.now());
            }
        }
        // Na konci metody se transakce commitne a Hibernate změny automaticky spláchne do DB.
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