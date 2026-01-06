package org.example.eshopbackend.mapper;

import org.example.eshopbackend.dto.CreateUserRequestDTO;
import org.example.eshopbackend.dto.UpdateUserRequestDTO;
import org.example.eshopbackend.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    User toUserEntity(CreateUserRequestDTO dto);


    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUser(@MappingTarget User target, UpdateUserRequestDTO dto);


}
