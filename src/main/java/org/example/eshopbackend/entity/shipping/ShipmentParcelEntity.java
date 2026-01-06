// src/main/java/org/example/zeniqbackend/entity/shipping/ShipmentParcelEntity.java
package org.example.eshopbackend.entity.shipping;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "shipment_parcels", indexes = {
        @Index(name = "ix_parcel_shipment", columnList = "shipment_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShipmentParcelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long parcelId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_parcel_shipment"))
    private ShipmentEntity shipment;

    @Column(name = "piece_index")
    private Integer pieceIndex;           // 1..N v rámci vícekusové zásilky

    @Column(name = "piece_tracking", length = 64)
    private String pieceTracking;         // pokud PPL dává kód na každý kus

    @Column(name = "label_path", length = 512)
    private String labelPath;             // pokud chceš uchovávat štítky po kusech

    @Column(name = "label_url", length = 1024)
    private String labelUrl;              // dočasné URL z PPL (pokud používáš)

    @Column(name = "label_mime", length = 32)
    private String labelMime;             // MIME typ
}
