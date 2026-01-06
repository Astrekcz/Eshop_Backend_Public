// src/main/java/org/example/zeniqbackend/entity/Product.java
package org.example.eshopbackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "productid", nullable = false, updatable = false)
    private Long productId;

    @Column(name = "productName", nullable = false, length = 160)
    private String productName;

    @Column(name = "slug", nullable = false, unique = true, length = 180)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "manufacturer", length = 120)
    private String manufacturer;

    @Column(name = "battery_capacity_mah", length = 64)
    private String batteryCapacityMah;

    @Column(name = "nicotine_mg", length = 64)
    private String nicotineMg;

    @Column(name = "volume", length = 64)
    private String volume;

    @Column(name = "puff_count", length = 64)
    private String puffCount;

    @Column(name = "flavor", length = 120)
    private String flavor;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @URL(message = "Neplatná URL adresa obrázku")
    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "amount_of_products", nullable = false)
    private Integer amount;

    // >>> Přidáno: galerie obrázků k produktu
    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,      // při smazání/uložení produktu se promítnou změny i do images
            orphanRemoval = true,           // odstranění z kolekce => DELETE v DB
            fetch = FetchType.LAZY
    )
    @OrderBy("sortOrder ASC, imageId ASC") // konzistentní pořadí
    private List<Image> images = new ArrayList<>();



    @Builder.Default
    @Column(name = "weight_grams")
    private Integer weightGrams = 0;

    // Helpery pro udržení konzistence obousměrné vazby
    public void addImage(Image image) {
        images.add(image);
        image.setProduct(this);
    }

    public void removeImage(Image image) {
        images.remove(image);
        image.setProduct(null);
    }
}
