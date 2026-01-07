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
        // Načteme všechny nedoručené zásilky
        var page = shipmentRepo.findByStatusNotIn(
                List.of(ShipmentStatus.DELIVERED, ShipmentStatus.CANCELLED, ShipmentStatus.ERROR),
                Pageable.unpaged()
        );

        List<ShipmentEntity> active = page.getContent();

        for (ShipmentEntity s : active) {
            try {
                // --- HLAVNÍ OPRAVA ---
                // Pokud zásilka nemá Tracking Number (tzn. je NEW/PAID a čeká na vyřízení),
                // tak ji OKAMŽITĚ PŘESKOČÍME.
                // Nikdy se neptáme PPL podle čísla objednávky, protože to vrací historii.
                if (s.getTrackingNumber() == null || s.getTrackingNumber().isBlank()) {
                    continue;
                }
                // ---------------------

                // Teď už víme, že máme Tracking Number, můžeme bezpečně volat PPL
                var st = pplClient.getStatus(s.getTrackingNumber());

                if (st == null) {
                    s.setUpdatedAt(Instant.now());
                    continue;
                }

                // Aktualizace stavu podle PPL
                s.setTrackingNumber(st.getTrackingNumber());
                s.setStatus(mapTrackingToShipment(st.getRawStatus()));
                s.setStatusText(st.getDescription());
                s.setUpdatedAt(Instant.now());

                log.debug("[PPL CRON] {} → {}", s.getTrackingNumber(), s.getStatusText());

            } catch (Exception ex) {
                log.warn("[PPL CRON] {} tracking failed: {}", s.getTrackingNumber(), ex.getMessage());
                // Pokud selže sync (výpadek PPL), označíme jen čas, neměníme status na ERROR,
                // aby se to zkusilo příště znovu.
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

        // Default, pokud nepoznáme stav, necháme to, co tam je (nebo REQUESTED)
        return ShipmentStatus.REQUESTED;
    }
}