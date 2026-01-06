package org.example.eshopbackend.dto.shipment;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateOrderDTO {

    private Long orderId;

    private String orderStatus;
}
