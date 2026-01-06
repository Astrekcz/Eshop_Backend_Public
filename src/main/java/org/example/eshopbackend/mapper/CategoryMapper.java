package org.example.eshopbackend.mapper;

import org.example.eshopbackend.dto.CategoryResponseDTO;
import org.example.eshopbackend.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {
    // Pokud máš CategoryResponseDTO, přidej ho sem:
     CategoryResponseDTO toDto(Category c);
}
