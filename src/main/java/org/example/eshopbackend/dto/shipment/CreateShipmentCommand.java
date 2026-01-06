package org.example.eshopbackend.dto.shipment;

import lombok.Data;

@Data
public class CreateShipmentCommand {
    /**
     * Počet krabic: 1=jednokus, >1=vícekus
     */
    private int piecesCount = 1;
    /**
     * Produkt/služba PPL, např. BUSS – dejte co máte ve smlouvě
     */
    private String productType = "BUSS";
    /**
     * Volitelně depot kód, např. "07" – pokud vyžaduje váš účet, jinak nechte null
     */
    private String depot;
    /**
     * Formát etikety: Pdf nebo Png (case-insensitive)
     */
    private String labelFormat = "Pdf";
    /**
     * DPI pro etiketu (většinou 300)
     */
    private Integer labelDpi = 300;
    /**
     * Chceš u vícekusu jedno společné URL pro všechny etikety
     */
    private boolean completeLabelRequested = true;
}