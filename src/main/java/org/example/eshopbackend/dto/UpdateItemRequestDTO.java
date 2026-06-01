package org.example.eshopbackend.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Map;

public record UpdateItemRequestDTO(
        @NotBlank
        @Size(max = 160)
        String name,

        String description,

        @NotNull
        @DecimalMin("0.0")
        BigDecimal price,

        @NotNull
        @Min(0)
        Integer stockAmount,

        @NotNull
        @Min(0)
        Integer weightGrams,

        @NotNull
        Long categoryId,

        Map<String, Object> attributes
) {}