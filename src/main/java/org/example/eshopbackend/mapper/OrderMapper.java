package org.example.eshopbackend.mapper;

import org.example.eshopbackend.dto.shipment.CreateOrderRequestDTO;
import org.example.eshopbackend.dto.shipment.OrderItemResponseDTO;
import org.example.eshopbackend.dto.shipment.OrderResponseDTO;
import org.example.eshopbackend.dto.shipment.UpdateOrderDTO;
import org.example.eshopbackend.entity.OrderEntity;
import org.example.eshopbackend.entity.OrderItemEntity;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    // Existující mapování - přidat explicitní mapování položek
    @Mapping(target = "items", source = "items")
    OrderResponseDTO toDto(OrderEntity order);

    // Přidat mapování pro jednotlivou položku
    OrderItemResponseDTO toItemDto(OrderItemEntity item);

    // Přidat mapování pro seznam položek
    List<OrderItemResponseDTO> toItemDtoList(List<OrderItemEntity> items);

    // Existující mapování - POZOR: items se nemapují z DTO, protože se řeší v service
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "orderNumber", ignore = true)
    @Mapping(target = "bankVs", ignore = true)
    @Mapping(target = "orderStatus", ignore = true)
    @Mapping(target = "subtotalCzk", ignore = true)
    @Mapping(target = "shippingCzk", ignore = true)
    @Mapping(target = "totalCzk", ignore = true)
    OrderEntity toEntity(CreateOrderRequestDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateOrder(@MappingTarget OrderEntity target, UpdateOrderDTO dto);
}