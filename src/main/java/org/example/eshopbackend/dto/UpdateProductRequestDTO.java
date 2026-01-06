// src/main/java/org/example/zeniqbackend/dto/UpdateProductRequestDTO.java
package org.example.eshopbackend.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateProductRequestDTO {

    @NotBlank @Size(max = 160)
    private String productName;

    private String description;
    private String manufacturer;
    private String batteryCapacityMah;
    private String nicotineMg;
    private String volume;
    private String puffCount;
    private String flavor;

    @NotNull
    private Integer amount;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal price;

    @NotNull
    private Long categoryId;

    @Min(0)
    @Max(100000)
    private Integer weightGrams; // může být null – pak se nemění


    @URL(message = "Neplatná URL adresa obrázku")
    private String imageUrl;

    // POZN.: slug se při update NEMĚNÍ (SEO). Kdybys ho chtěl měnit, přidej sem volitelné pole.
}
