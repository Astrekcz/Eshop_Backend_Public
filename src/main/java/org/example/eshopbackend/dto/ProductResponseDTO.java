package org.example.eshopbackend.dto;

import lombok.*;
import java.math.BigDecimal;

// src/main/java/org/example/zeniqbackend/dto/ProductResponseDTO.java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductResponseDTO {

    private Long productId;
    private String productName;
    private String slug;

    private String description;

    // String parametry produktu
    private String manufacturer;
    private String batteryCapacityMah;
    private String nicotineMg;
    private String volume;
    private String puffCount;
    private String flavor;

    private Integer amount;
    private BigDecimal price;

    // info o kategorii
    private Long categoryId;
    private String categoryName;

    /** URL hlavního obrázku z Image entity (primary → sortOrder → imageId) */
    private String primaryImageUrl;

    private Integer weightGrams;

    /** legacy – už nepoužívat pro FE */
    @Deprecated
    private String imageUrl;
}

