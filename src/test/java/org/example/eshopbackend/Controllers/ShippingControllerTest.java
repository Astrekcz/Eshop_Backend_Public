// src/test/java/org/example/zeniqbackend/Controllers/ShippingControllerTest.java
package org.example.eshopbackend.Controllers;

import org.example.eshopbackend.controllers.ShippingController;
import org.example.eshopbackend.dto.shipment.ShipmentDTO;
import org.example.eshopbackend.shipping.ppl.ShipmentService;
import org.example.eshopbackend.security.JwtRequestFilter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.annotation.Resource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@WebMvcTest(
        controllers = ShippingController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtRequestFilter.class),
                // pokud máš vlastní SecurityConfig, můžeš taky vyloučit:
                // @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class ShippingControllerTest {

    @Resource private MockMvc mvc;

    @MockitoBean
    private ShipmentService shipmentService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getShipment_returnsDto() throws Exception {
        var dto = ShipmentDTO.builder()
                .shipmentId(10L)
                .trackingNumber("TEST123")
                .status("IN_TRANSIT")
                .statusText("Na cestě")
                .build();

        Mockito.when(shipmentService.refreshTracking(10L)).thenReturn(dto);

        mvc.perform(get("/api/shipping/shipments/10").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipmentId").value(10))
                .andExpect(jsonPath("$.trackingNumber").value("TEST123"))
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"));
    }
}
