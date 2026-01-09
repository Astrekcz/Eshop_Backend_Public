package org.example.eshopbackend.Controllers;
import org.example.eshopbackend.controllers.ShippingController;
import org.example.eshopbackend.dto.shipment.ShipmentDTO;
import org.example.eshopbackend.security.JwtAuthenticationFilter;
import org.example.eshopbackend.security.SecurityConfig;
import org.example.eshopbackend.shipping.ppl.ShipmentService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // Spring Boot 3.4+
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@WebMvcTest(
        controllers = ShippingController.class,
        excludeFilters = {
                // 1. Vyloučíme novou Security konfiguraci (aby test nechtěl JwtService, UserDetails atd.)
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
                // 2. Vyloučíme nový filtr
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
        }
)
// 3. addFilters = false vypne Security úplně (pro unit test controlleru ideální)
@AutoConfigureMockMvc(addFilters = false)
class ShippingControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ShipmentService shipmentService;

    @Test
        // @WithMockUser zde není potřeba, protože addFilters=false vypne ověřování rolí
        // Pokud bys chtěl testovat i Security, musel bys zapnout filtry a namockovat JwtService.
    void getShipment_returnsDto() throws Exception {
        var dto = ShipmentDTO.builder()
                .shipmentId(10L)
                .trackingNumber("TEST123")
                .status("IN_TRANSIT")
                .statusText("Na cestě")
                .build();

        Mockito.when(shipmentService.refreshTracking(10L)).thenReturn(dto);

        mvc.perform(get("/api/shipping/shipments/10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipmentId").value(10))
                .andExpect(jsonPath("$.trackingNumber").value("TEST123"))
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"));
    }
}