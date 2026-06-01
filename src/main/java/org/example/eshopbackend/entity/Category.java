package org.example.eshopbackend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class Category extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String categoryName;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;  //jednoznazny url identifikator misto ciselneho ID se v adrese objevi slova kategorie

}
