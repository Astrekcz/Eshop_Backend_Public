package org.example.eshopbackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

// 1. Dědíme z BaseEntity - tím získáme id, createdAt a updatedAt automaticky
@Entity
@Table(name = "images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Image extends BaseEntity {

    // 2. imageId a createdAt SMAZÁNY - jsou už v BaseEntity (id, createdAt)

    // 3. Stará vazba nastavena jako volitelná a NEZAPISOVATELNÁ (jen pro čtení starých dat)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    @Deprecated
    private Product product;

    // 4. Nová vazba na Item - tohle je teď hlavní majitel vztahu
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @NotBlank
    @Column(name = "url", nullable = false, length = 512)
    private String url;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    /** Pořadí zobrazení v galerii (0 = první) */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    // 5. prePersist SMAZÁN - BaseEntity už má vlastní @PrePersist logiku
}