// src/main/java/org/example/zeniqbackend/mapper/ShipmentMapper.java
package org.example.eshopbackend.mapper;

import org.example.eshopbackend.dto.shipment.CreateShipmentCommand;
import org.example.eshopbackend.dto.shipment.PplCreateShipmentRequestDTO;
import org.example.eshopbackend.dto.shipment.ShipmentDTO;
import org.example.eshopbackend.entity.OrderEntity;
import org.example.eshopbackend.entity.shipping.ShipmentEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ShipmentMapper {

    // === Order + admin command -> request pro PPL ===
    @Mapping(target = "orderNumber",      source = "order.orderNumber")
    @Mapping(target = "serviceCode",      source = "cmd.productType")
    @Mapping(target = "recipientName",    expression = "java(order.getCustomerFirstName() + \" \" + order.getCustomerLastName())")
    @Mapping(target = "recipientStreet",  expression = "java(buildStreet(order))")
    @Mapping(target = "recipientCity",    source = "order.shipCity")
    @Mapping(target = "recipientZip",     source = "order.shipPostalCode")
    @Mapping(target = "recipientCountry", source = "order.shipCountryCode")
    @Mapping(target = "recipientEmail",   source = "order.customerEmail")
    @Mapping(target = "recipientPhone",   source = "order.customerPhone")
    @Mapping(target = "codAmountKc",      constant = "0") // bez dobírky
    @Mapping(target = "weightGrams", expression = "java(totalWeightGrams(order, packagingWeightGrams))")
    PplCreateShipmentRequestDTO toPplRequest(OrderEntity order, CreateShipmentCommand cmd, @Context int packagingWeightGrams);

    // === Entity -> DTO pro FE/admin ===
    @Mapping(target = "shipmentId",       source = "shipmentId")
    @Mapping(target = "orderNumber",      source = "order.orderNumber")
    @Mapping(target = "trackingNumber",   source = "trackingNumber")
    @Mapping(target = "pplBatchId",       source = "pplBatchId")
    @Mapping(target = "piecesCount",      source = "piecesCount")
    @Mapping(target = "status",           expression = "java(entity.getStatus() == null ? null : entity.getStatus().name())")
    @Mapping(target = "statusText",       source = "statusText")
    @Mapping(target = "labelDownloadUrl", expression = "java(\"/api/shipping/shipments/\" + entity.getShipmentId() + \"/label\")")
    ShipmentDTO toDto(ShipmentEntity entity);

    // === Helpers ===
    default String buildStreet(OrderEntity o) {
        String orient = (o.getShipOrientationNumber() == null || o.getShipOrientationNumber().isBlank())
                ? "" : "/" + o.getShipOrientationNumber();
        return o.getShipStreet() + " " + o.getShipHouseNumber() + orient;
    }

    /** Součet kusů v objednávce. */
    default int totalPieces(OrderEntity order) {
        if (order == null || order.getItems() == null) return 0;
        int pcs = 0;
        for (var it : order.getItems()) {
            pcs += Math.max(0, it.getAmountOfProducts());
        }
        return pcs;
    }

    /** Orientační váha obalu dle počtu kusů. U jednorázovek stačí konzervativní tier. */
    default int packagingWeightGrams(OrderEntity order) {
        int pcs = totalPieces(order);
        if (pcs <= 5)  return 150;  // malá krabice + výplň
        if (pcs <= 12) return 250;  // střední
        return 350;                  // větší
    }

    /** Celková reálná hmotnost: součet (qty * weightGrams) + váha obalu. */
    default int totalWeightGrams(OrderEntity order, int packagingWeightGrams) {
        int items = 0;
        if (order != null && order.getItems() != null) {
            for (var it : order.getItems()) {
                int w = Math.max(0, it.getWeightGrams() == null ? 0 : it.getWeightGrams());
                int q = Math.max(0, it.getAmountOfProducts());
                items += w * q;
            }
        }
        int total = Math.max(1, items + Math.max(0, packagingWeightGrams));
        return total;
    }
}
