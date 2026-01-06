package org.example.eshopbackend.shipping.ppl;


import org.example.eshopbackend.dto.shipment.PplCreateShipmentRequestDTO;
import org.example.eshopbackend.dto.shipment.PplCreateShipmentResultDTO;
import org.example.eshopbackend.dto.shipment.PplTrackingStatus;

public interface PplClient {

    /**
     * Vytvoří zásilku u PPL a vrátí tracking + stažený štítek.
     *
     * @param req      základní údaje (reference, recipient…)
     * @param pieces   1=jednokus, >1=vícekus (mapuj na shipmentSet.numberOfShipments)
     * @param format   "Pdf" nebo "Png" (case insensitive)
     * @param dpi      např. 300
     * @param completeLabelRequested u vícekusu společné URL na etikety (pokud to PPL podporuje)
     * @param depot    volitelné – pošli jen pokud to váš účet vyžaduje
     */
    PplCreateShipmentResultDTO createShipmentWithLabel(
            PplCreateShipmentRequestDTO req,
            int pieces,
            String format,
            Integer dpi,
            boolean completeLabelRequested,
            String depot
    );

    /**
     * Načte stav zásilky (podle tracking number nebo přes batch/ship id – uprav v implementaci).
     */
    PplTrackingStatus getStatus(String trackingNumber);

}