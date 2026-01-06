package org.example.eshopbackend.serviceTest;
import org.example.eshopbackend.dto.shipment.PplTrackingStatus;
import org.example.eshopbackend.dto.shipment.ShipmentDTO;
import org.example.eshopbackend.entity.OrderEntity;
import org.example.eshopbackend.entity.shipping.ShipmentEntity;
import org.example.eshopbackend.entity.shipping.ShipmentStatus;
import org.example.eshopbackend.mapper.ShipmentMapper;
import org.example.eshopbackend.repository.OrderRepository;
import org.example.eshopbackend.repository.ShipmentRepository;
import org.example.eshopbackend.shipping.ppl.PplClient;
import org.example.eshopbackend.shipping.ppl.ShipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ShipmentServiceTest {

    @Mock private OrderRepository orderRepo;
    @Mock private ShipmentRepository shipmentRepo;
    @Mock private ShipmentMapper mapper;
    @Mock private PplClient pplClient;

    @InjectMocks
    private ShipmentService service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void refreshTracking_withTrackingNumber_updatesStatusAndText() {
        // given: shipment v DB s TN
        ShipmentEntity entity = new ShipmentEntity();
        entity.setShipmentId(1L);
        entity.setTrackingNumber("TEST123");
        entity.setStatus(ShipmentStatus.REQUESTED);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        when(shipmentRepo.findById(1L)).thenReturn(Optional.of(entity));

        // a PPL vrátí IN_TRANSIT
        var ppl = PplTrackingStatus.builder()
                .trackingNumber("TEST123")
                .rawStatus("IN_TRANSIT")
                .description("Na cestě")
                .build();
        when(pplClient.getStatus("TEST123")).thenReturn(ppl);

        // mapper vrátí jednoduché DTO
        when(mapper.toDto(any(ShipmentEntity.class))).thenAnswer(inv -> {
            ShipmentEntity e = inv.getArgument(0);
            return ShipmentDTO.builder()
                    .shipmentId(e.getShipmentId())
                    .trackingNumber(e.getTrackingNumber())
                    .status(e.getStatus() != null ? e.getStatus().name() : null)
                    .statusText(e.getStatusText())
                    .pplBatchId(e.getPplBatchId())
                    .piecesCount(e.getPiecesCount())
                    .build();
        });

        // when
        ShipmentDTO dto = service.refreshTracking(1L);

        // then
        assertThat(dto.getTrackingNumber()).isEqualTo("TEST123");
        assertThat(dto.getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT.name());
        assertThat(dto.getStatusText()).isEqualTo("Na cestě");
        verify(pplClient).getStatus("TEST123");
        verify(shipmentRepo, atLeastOnce()).save(any(ShipmentEntity.class));
    }

    @Test
    void refreshTracking_withoutTrackingNumber_doesNotCallPplAndJustTouchesUpdatedAt() {
        // given: shipment bez TN
        ShipmentEntity entity = new ShipmentEntity();
        entity.setShipmentId(2L);
        entity.setTrackingNumber(null);
        entity.setStatus(ShipmentStatus.REQUESTED);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        when(shipmentRepo.findById(2L)).thenReturn(Optional.of(entity));

        when(mapper.toDto(any())).thenAnswer(inv -> {
            ShipmentEntity e = inv.getArgument(0);
            return ShipmentDTO.builder()
                    .shipmentId(e.getShipmentId())
                    .trackingNumber(e.getTrackingNumber())
                    .status(e.getStatus() != null ? e.getStatus().name() : null)
                    .statusText(e.getStatusText())
                    .build();
        });

        // when
        ShipmentDTO dto = service.refreshTracking(2L);

        // then
        assertThat(dto.getTrackingNumber()).isNull();
        verifyNoInteractions(pplClient);
        verify(shipmentRepo, atLeastOnce()).save(any());
    }

    @Test
    void findDtoByOrderNumber_whenMissing_returnsEmpty() {
        when(shipmentRepo.findByOrder_OrderNumber("ORD-404")).thenReturn(Optional.empty());
        var res = service.findDtoByOrderNumber("ORD-404");
        assertThat(res).isEmpty();
        verifyNoInteractions(pplClient);
    }
}