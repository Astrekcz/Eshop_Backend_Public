package org.example.eshopbackend.mapper;

import org.example.eshopbackend.dto.CategoryResponseDTO;
import org.example.eshopbackend.dto.UpdateCategoryRequestDTO;
import org.example.eshopbackend.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {
    // Pokud máš CategoryResponseDTO, přidej ho sem:
     CategoryResponseDTO toDto(Category c);


     @Mapping(target = "id", ignore = true )
     void updateEntity(@MappingTarget Category target, UpdateCategoryRequestDTO dto);

}
