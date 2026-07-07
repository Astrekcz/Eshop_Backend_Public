package org.example.eshopbackend.dto.shipment;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PplCreateShipmentResultDTO {
    // ID/dávka/ship id – jak to PPL pojmenovává; když máš jen jedno, používej shipmentId
    private String id;
    private String batchId;            // pokud PPL vrací zvlášť batch ID
    private String trackingNumber;

    // štítek
    private byte[] labelPdf;           // nebo PNG/BIN, název pole nechávám "Pdf" pro jednoduchost
    private String labelMime;          // "application/pdf" / "image/png"
}