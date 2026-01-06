package org.example.eshopbackend.dto.shipment;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class PplTrackingStatus {
    private String trackingNumber;
    private String rawStatus;      // např. "IN_TRANSIT", "DELIVERED"...
    private String description;    // čitelný popis posledního stavu
    private Instant lastEventTime; // volitelné
    private String lastHub;        // volitelné
}