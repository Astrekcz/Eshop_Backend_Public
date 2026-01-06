// src/main/java/org/example/zeniqbackend/dto/shipment/OrderItemRequestDTO.java
package org.example.eshopbackend.dto.shipment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class OrderItemRequestDTO {
    @NotNull
    private Long productId;

    @Min(1)
    private int quantity;
}
