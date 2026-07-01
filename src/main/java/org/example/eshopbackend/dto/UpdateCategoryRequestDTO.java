package org.example.eshopbackend.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCategoryRequestDTO(

        @NotBlank
        String categoryName,

        @NotBlank
        String slug
) {}
