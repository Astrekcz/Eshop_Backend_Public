package org.example.eshopbackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;

    @Column(nullable = false, length = 100)
    private String categoryName;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;  //jednoznazny url identifikator misto ciselneho ID se v adrese objevi slova kategorie

}
