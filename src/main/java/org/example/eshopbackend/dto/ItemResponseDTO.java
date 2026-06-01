package org.example.eshopbackend.dto;

import org.example.eshopbackend.dto.image.ImageResponseDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ItemResponseDTO(
        Long id,
        String name,
        String slug,
        String description,
        BigDecimal price,
        Integer stockAmount,
        Integer weightGrams,
        Long categoryId,
        String categoryName,
        Map<String, Object> attributes,
        List<ImageResponseDTO> images, // Odkazuje na samostatný soubor
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}