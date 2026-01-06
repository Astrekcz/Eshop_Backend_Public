// src/main/java/org/example/zeniqbackend/entity/shipping/ShipmentEntity.java
package org.example.eshopbackend.entity.shipping;

import jakarta.persistence.*;
import lombok.*;
import org.example.eshopbackend.entity.OrderEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shipments", indexes = {
        @Index(name = "ix_ship_order", columnList = "order_id"),
        @Index(name = "ix_ship_batch", columnList = "ppl_batch_id"),
        @Index(name = "ix_ship_tracking", columnList = "tracking_number")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShipmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long shipmentId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_shipment_order"))
    private OrderEntity order;

    // Identifikátory od PPL
    @Column(name = "ppl_batch_id", length = 64)
    private String pplBatchId;            // batchID z POST shipment/batch

    @Column(name = "ppl_shipment_id", length = 64)
    private String pplShipmentId;         // interní shipmentId (pokud PPL vrací)

    @Column(name = "tracking_number", length = 64)
    private String trackingNumber;        // hlavní tracking

    // Nastavení služby
    @Column(name = "product_type", length = 32)
    private String productType;           // např. "BUSS"

    @Column(name = "depot", length = 8)
    private String depot;                 // pokud vyžaduje smlouva

    // Vícekusová info
    @Column(name = "pieces_count")
    private Integer piecesCount;          // 1 = jednokus, >1 = vícekus (shipmentSet.numberOfShipments)

    // Etiketa (uložené PDF/PNG u nás)
    @Column(name = "label_path", length = 512)
    private String labelPath;             // kam jsi uložil PDF (disk/S3)

    @Column(name = "label_mime", length = 32)
    private String labelMime;             // "application/pdf" / "image/png"

    // Volitelné: pokud si dočasně držíš URL od PPL
    @Column(name = "label_url", length = 1024)
    private String labelUrl;              // může expirovat; proto preferuj labelPath

    // Stav a poslední synchronizace
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private ShipmentStatus status;

    @Column(name = "status_text", length = 255)
    private String statusText;            // čitelný popis posledního stavu

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    // Parcely pro vícekus (volitelné)
    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ShipmentParcelEntity> parcels = new ArrayList<>();

    // Audit
    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
        if (status == null) status = ShipmentStatus.NEW;
        if (piecesCount == null) piecesCount = 1;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
