package org.example.eshopbackend.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProductRequestDTO {

    @NotBlank
    @Size(max = 160)
    private String productName;

    // volitelné (v DB je TEXT bez nullable=false)
    private String description;

    // String parametry z entity (volitelné)
    private String manufacturer;
    private String batteryCapacityMah;
    private String nicotineMg;
    private String volume;
    private String puffCount;
    private String flavor;

    @NotNull
    private Integer amount;

    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal price;

    // vazba na Category.categoryId
    @NotNull
    private Long categoryId;

    @NotNull
    @Min(0)
    @Max(100000) // 100 kg limit jako bezpečný strop
    private Integer weightGrams;


    @URL(message = "Neplatná URL adresa obrázku")
    private String imageUrl;

    // pokud slug generuješ na backendu, klidně tenhle field vynech


}
