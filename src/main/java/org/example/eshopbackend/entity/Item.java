package org.example.eshopbackend.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Item extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, unique = true, length = 180)
    private String slug;

    private String description;
    private BigDecimal price;
    private Integer stockAmount;
    private Integer weightGrams;

    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;


    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> attributes = new HashMap<>();

    // >>> Přidáno: galerie obrázků k produktu
    @OneToMany(
            mappedBy = "item",
            cascade = CascadeType.ALL,      // při smazání/uložení produktu se promítnou změny i do images
            orphanRemoval = true,           // odstranění z kolekce => DELETE v DB
            fetch = FetchType.LAZY
    )
    @OrderBy("sortOrder ASC, Id ASC") // konzistentní pořadí
    private List<Image> images = new ArrayList<>();


    // Helpery pro udržení konzistence obousměrné vazby
    public void addImage(Image image) {
        images.add(image);
        image.setItem(this);
    }

    public void removeImage(Image image) {
        images.remove(image);
        image.setItem(null);
    }
}


