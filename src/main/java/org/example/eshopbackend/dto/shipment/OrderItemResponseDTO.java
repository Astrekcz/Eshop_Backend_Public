package org.example.eshopbackend.dto.shipment;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
public class OrderItemResponseDTO {
    private Long oItemId;
    private String nameOfProduct;
    private String name;
    private int amountOfProducts;
    private BigDecimal unitPriceCzk;
    private BigDecimal lineTotalCzk;
    private Integer weightGrams;  // hmotnost v gramech
}