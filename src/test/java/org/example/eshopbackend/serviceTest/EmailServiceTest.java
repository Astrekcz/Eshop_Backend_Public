package org.example.eshopbackend.serviceTest;

import jakarta.mail.internet.MimeMessage;
import org.example.eshopbackend.entity.OrderEntity;
import org.example.eshopbackend.entity.OrderItemEntity;
import org.example.eshopbackend.service.email.EmailService;
import org.example.eshopbackend.util.QrGenerator;
import org.example.eshopbackend.util.Spayd;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        // 1. Nastavení chování mocku pro vytvoření zprávy
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // 2. Injektování @Value hodnot pomocí ReflectionTestUtils (protože neběží Spring kontext)
        ReflectionTestUtils.setField(emailService, "from", "info@eshop.cz");
        ReflectionTestUtils.setField(emailService, "iban", "CZ1234567890");
        ReflectionTestUtils.setField(emailService, "sellerName", "Můj Eshop");
        ReflectionTestUtils.setField(emailService, "sellerEmail", "kontakt@eshop.cz");
        ReflectionTestUtils.setField(emailService, "termsClasspath", "neexistujici_cesta.pdf"); // Aby se netestovalo načítání souboru
    }

    private OrderEntity createDummyOrder() {
        OrderEntity order = new OrderEntity();
        order.setOrderNumber("2025001");
        order.setCustomerEmail("zakaznik@example.com");
        order.setCustomerFirstName("Jan");
        order.setCustomerLastName("Novák");

        // ZMĚNA: Používáme Long (L) místo BigDecimal
        order.setTotalCzk(1500L);
        order.setSubtotalCzk(1400L);
        order.setShippingCzk(100L);

        order.setBankVs("2025001");
        // Položky
        OrderItemEntity item = new OrderItemEntity();
        item.setName("Testovací produkt");
        item.setAmountOfProducts(1);
        item.setUnitPriceCzk(new BigDecimal("1400"));
        item.setLineTotalCzk(new BigDecimal("1400"));

        order.setItems(List.of(item));
        return order;
    }

    @Test
    void testSendOrderConfirmation() {
        // Arrange
        OrderEntity order = createDummyOrder();

        // Protože metoda volá statické metody (QrGenerator, Spayd),
        // v unit testu se normálně provedou. Pokud jsou složité, museli bychom použít mockStatic.
        // Zde předpokládáme, že QrGenerator a Spayd fungují bez externích závislostí.
        // Pokud by QrGenerator dělal problémy, lze to obalit do try-with-resources mockStatic.

        try (MockedStatic<QrGenerator> qrMock = Mockito.mockStatic(QrGenerator.class);
             MockedStatic<Spayd> spaydMock = Mockito.mockStatic(Spayd.class)) {

            // Mockování statických metod, aby test neřešil grafiku a formáty
            spaydMock.when(() -> Spayd.build(any(), any(), any(), any(), any()))
                    .thenReturn("SPAYDString");
            qrMock.when(() -> QrGenerator.toPng(anyString(), anyInt()))
                    .thenReturn(new byte[0]); // Vrátíme prázdné pole bytů

            // Act
            emailService.sendOrderConfirmation(order);

            // Assert
            verify(mailSender, times(1)).send(mimeMessage);

            // Můžeme ověřit, že se nastavil Subject (i když MimeMessage je mock,
            // MimeMessageHelper na něm volá setSubject, což by mělo jít ověřit, pokud to mock dovolí,
            // ale jednodušší je ověřit, že nedošlo k chybě a metoda send se zavolala).
        }
    }

    @Test
    void testSendOrderShipped() {
        // Arrange
        OrderEntity order = createDummyOrder();

        // Act
        emailService.sendOrderShipped(order);

        // Assert
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void testSendShipmentHandedOver() {
        // Arrange
        OrderEntity order = createDummyOrder();
        String trackingNumber = "12345678901";
        String trackingUrl = "https://ppl.cz/xxx";

        // Act
        emailService.sendShipmentHandedOver(order, trackingNumber, trackingUrl);

        // Assert
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void testSendPaymentReceived() {
        // Arrange
        OrderEntity order = createDummyOrder();

        // Act
        emailService.sendPaymentReceived(order);

        // Assert
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void testHandlingException() {
        // Arrange
        OrderEntity order = createDummyOrder();
        // Simulujeme chybu při odesílání
        doThrow(new RuntimeException("SMTP Error")).when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            emailService.sendPaymentReceived(order);
        });

        assertEquals("Nepodařilo se poslat potvrzení o platbě", thrown.getMessage());
    }
}