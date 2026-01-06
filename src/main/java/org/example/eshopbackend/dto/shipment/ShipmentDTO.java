package org.example.eshopbackend.dto.shipment;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class ShipmentDTO {
    private Long shipmentId;
    private String orderNumber;
    private String trackingNumber;
    private String pplBatchId;
    private Integer piecesCount;
    private String status;
    private String statusText;
    /** URL na stažení uloženého PDF z našeho BE */
    private String labelDownloadUrl;
}