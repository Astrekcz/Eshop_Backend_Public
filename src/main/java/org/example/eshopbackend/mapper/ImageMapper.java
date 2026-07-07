// src/main/java/org/example/zeniqbackend/mapper/ImageMapper.java
package org.example.eshopbackend.mapper;

import org.example.eshopbackend.dto.image.ImageResponseDTO;
import org.example.eshopbackend.entity.Image;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ImageMapper {

    @Mapping(source = "item.id", target = "Id")
    ImageResponseDTO toDto(Image image);
}
