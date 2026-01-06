package org.example.eshopbackend.dto.shipment;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderResponseDTO {

    private Long orderId;
    private String orderNumber;
    private String customerFirstName;
    private String customerLastName;
    private String customerEmail;
    private String customerPhone;

    private String shipStreet;
    private String shipHouseNumber;
    private String shipOrientationNumber;
    private String shipCity;
    private String shipPostalCode;
    private String shipCountryCode;

    private boolean ageConfirmed;

    private Long subtotalCzk;
    private Long shippingCzk;
    private Long totalCzk;

    private String bankVs;

    private String orderStatus; // tady ano!
    private String trackingNumber;


    private List<OrderItemResponseDTO> items;
}
