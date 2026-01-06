// src/main/java/org/example/zeniqbackend/mapper/ProductMapper.java
package org.example.eshopbackend.mapper;

import org.example.eshopbackend.dto.CreateProductRequestDTO;
import org.example.eshopbackend.dto.ProductResponseDTO;
import org.example.eshopbackend.dto.UpdateProductRequestDTO;
import org.example.eshopbackend.entity.Category;
import org.example.eshopbackend.entity.Product;
import org.mapstruct.*;

// ProductMapper.java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "weightGrams", source = "weightGrams") // ⬅ POVINNĚ
    Product toEntity(CreateProductRequestDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "weightGrams", source = "weightGrams") // ⬅ POVINNĚ
    void updateEntity(@MappingTarget Product target, UpdateProductRequestDTO dto);

    @Mapping(target = "categoryId", source = "category.categoryId")
    @Mapping(target = "categoryName", source = "category.categoryName")
    ProductResponseDTO toDto(Product product);

    default Product toEntity(CreateProductRequestDTO dto, Category category, String slug) {
        Product p = toEntity(dto);
        p.setCategory(category);
        p.setSlug(slug);
        return p;
    }
}
