package org.example.eshopbackend.serviceTest;

import org.example.eshopbackend.dto.shipment.CreateOrderRequestDTO;
import org.example.eshopbackend.entity.OrderEntity;
import org.example.eshopbackend.exception.OrderCreationException;
import org.example.eshopbackend.mapper.OrderMapper;
import org.example.eshopbackend.repository.OrderRepository;
import org.example.eshopbackend.service.email.EmailService;
import org.example.eshopbackend.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderMapper orderMapper;
    @Mock EmailService emailService;
    @Mock org.example.eshopbackend.adulto.AdultoVerifier adultoVerifier;

    @InjectMocks OrderService orderService;

    @Test
    void addOrder_adultOK_saves() {
        // Arrange
        var dto = new CreateOrderRequestDTO();
        dto.setCustomerFirstName("Jan");
        dto.setCustomerLastName("Novák");
        dto.setCustomerEmail("jan@novak.cz");
        dto.setShipStreet("Ulice");
        dto.setShipHouseNumber("12");
        dto.setShipCity("Praha");
        dto.setShipPostalCode("11000");
        dto.setSubtotalCzk(1000L);
        dto.setShippingCzk(0L);
        dto.setTotalCzk(1000L);
        dto.setAdultoczUid("DEV-MOCK-UID-OK");

        // mapper musí vrátit novou entitu (simulace mapování)
        OrderEntity mapped = new OrderEntity();
        when(orderMapper.toEntity(any(CreateOrderRequestDTO.class))).thenReturn(mapped);

        when(adultoVerifier.verify("DEV-MOCK-UID-OK", 18))
                .thenReturn(new org.example.eshopbackend.adulto.AdultoVerification(
                        true, true, "18", "MOCK", "DEV-MOCK-UID-OK"
                ));

        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> {
            OrderEntity e = inv.getArgument(0);
            e.setOrderId(1L);
            return e;
        });

        // Act
        OrderEntity out = orderService.addOrder(dto);

        // Assert
        assertTrue(out.isAgeConfirmed());
        verify(orderRepository).save(argThat(e -> e.isAgeConfirmed()));
        verify(emailService).sendOrderConfirmation(any(OrderEntity.class));
    }

    @Test
    void addOrder_adultNOK_throws() {
        var dto = new CreateOrderRequestDTO();
        dto.setCustomerFirstName("Jan");
        dto.setCustomerLastName("Novák");
        dto.setCustomerEmail("jan@novak.cz");
        dto.setShipStreet("Ulice");
        dto.setShipHouseNumber("12");
        dto.setShipCity("Praha");
        dto.setShipPostalCode("11000");
        dto.setSubtotalCzk(1000L);
        dto.setShippingCzk(0L);
        dto.setTotalCzk(1000L);
        // adultoczUid necháme null → service zavolá verifier s null

        //  NIC nestubuj pro mapper – nebude se volat
        when(adultoVerifier.verify(null, 18))
                .thenReturn(new org.example.eshopbackend.adulto.AdultoVerification(
                        true, false, "18", "MOCK", null
                ));

        assertThrows(OrderCreationException.class, () -> orderService.addOrder(dto));
        verify(orderRepository, never()).save(any());
        verify(emailService, never()).sendOrderConfirmation(any());
        verifyNoInteractions(orderMapper); // volitelné: pojisti, že se mapper opravdu nevolal
    }
    @Test
    void addOrder_countryCesko_isAcceptedByServiceWithMocks() {
        // Arrange
        var dto = new CreateOrderRequestDTO();
        dto.setCustomerFirstName("Jan");
        dto.setCustomerLastName("Novák");
        dto.setCustomerEmail("jan@novak.cz");
        dto.setShipStreet("Ulice");
        dto.setShipHouseNumber("12");
        dto.setShipCity("Praha");
        dto.setShipPostalCode("11000");

        // klíčové: pošleme "Česko"
        dto.setShipCountryCode("Česko");

        dto.setSubtotalCzk(1000L);
        dto.setShippingCzk(0L);
        dto.setTotalCzk(1000L);
        dto.setAdultoczUid("DEV-MOCK-UID-OK");

        when(adultoVerifier.verify("DEV-MOCK-UID-OK", 18))
                .thenReturn(new org.example.eshopbackend.adulto.AdultoVerification(
                        true, true, "18", "MOCK", "DEV-MOCK-UID-OK"
                ));

        when(orderMapper.toEntity(any(CreateOrderRequestDTO.class))).thenReturn(new OrderEntity());
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> {
            OrderEntity e = inv.getArgument(0);
            e.setOrderId(1L);
            return e;
        });

        // Act + Assert: v této testovací konfiguraci (mock DB) to "Česko" projde
        assertDoesNotThrow(() -> orderService.addOrder(dto));
    }

}
