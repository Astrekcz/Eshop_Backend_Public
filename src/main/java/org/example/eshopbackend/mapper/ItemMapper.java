package org.example.eshopbackend.mapper;

import org.example.eshopbackend.dto.CreateItemRequestDTO;
import org.example.eshopbackend.dto.ItemResponseDTO;
import org.example.eshopbackend.dto.UpdateItemRequestDTO;
import org.example.eshopbackend.entity.Item;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
builder = @Builder(disableBuilder = false))
public interface ItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "images", ignore = true)
    Item toEntity(CreateItemRequestDTO dto);

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.categoryName")
    ItemResponseDTO toDto(Item item);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "images", ignore = true)
    void updateEntity(@MappingTarget Item target, UpdateItemRequestDTO dto);
}