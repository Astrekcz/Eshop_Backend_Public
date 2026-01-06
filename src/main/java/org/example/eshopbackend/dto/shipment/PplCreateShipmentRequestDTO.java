package org.example.eshopbackend.dto.shipment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PplCreateShipmentRequestDTO {
    // tvoje reference (např. orderNumber)
    private String orderNumber;

    // služba/produkt (BUSS apod.)
    private String serviceCode;

    // adresát
    private String recipientName;
    private String recipientStreet;
    private String recipientCity;
    private String recipientZip;
    private String recipientCountry;
    private String recipientEmail;
    private String recipientPhone;

    // volitelné – podle potřeby doplníš v PplClient implementaci:
    private Integer weightGrams;   // pokud nemáš, klidně 0
    private Integer codAmountKc;   // bez dobírky = 0
}