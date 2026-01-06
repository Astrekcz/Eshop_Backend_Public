package org.example.eshopbackend.dto.shipment;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.example.eshopbackend.validation.ValidPhone;

import java.util.List;


@Getter
@Setter
public class CreateOrderRequestDTO {

    @NotNull
    private String customerFirstName;

    @NotNull
    private String customerLastName;

    @NotNull
    private String customerEmail;

    @NotNull
    @ValidPhone(defaultRegion = "CZ")
    private String customerPhone;

    @NotNull
    private String shipStreet;

    @NotNull
    private String shipHouseNumber;

    private String shipOrientationNumber;

    @NotNull
    private String shipCity;

    @NotNull
    private String shipPostalCode;

    @NotNull
    private String shipCountryCode;


    private boolean ageConfirmed;

    @NotNull
    private Long subtotalCzk;

    @NotNull
    private Long shippingCzk;

    @NotNull
    private Long totalCzk;

    private String bankVs; // variabilní symbol (např. číslo objednávky)

   
    private String adultoczUid;

    @NotEmpty
    private List<OrderItemRequestDTO> items;
}
