package org.example.eshopbackend.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Map;

public record CreateItemRequestDTO(
        @NotBlank(message = "Název je povinný")
        @Size(max = 160)
        String name,

        String description,

        @NotNull(message = "Cena nesmí být prázdná")
        @DecimalMin(value = "0.0", message = "Cena nesmí být záporná")
        BigDecimal price,

        @NotNull(message = "Množství na skladě je povinné")
        @Min(0)
        Integer stockAmount,

        @NotNull(message = "Hmotnost je povinná")
        @Min(0)
        Integer weightGrams,

        @NotNull(message = "Kategorie je povinná")
        Long categoryId,

        // Dynamické atributy z JSONB
        Map<String, Object> attributes
) {}