package com.odyssey.api.booking.confirmation;

import com.odyssey.api.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = SupplierPaymentSimulationController.class,
    properties = "app.supplier-payment-simulation.enabled=true"
)
@ActiveProfiles({"dev", "production"})
@Import(SecurityConfig.class)
class SupplierPaymentSimulationProductionHttpTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private SupplierPaymentSimulationService service;
    @MockitoBean private JwtDecoder decoder;

    @Test
    void productionHasNoSimulationEndpointsEvenWithDevProfileAndFlagEnabled() throws Exception {
        var now = Instant.now();
        when(decoder.decode("owner")).thenReturn(new Jwt(
            "owner", now, now.plusSeconds(3600), Map.of("alg", "none"),
            Map.of("sub", "owner", "https://odyssey.app/roles", List.of("TRAVELER"))
        ));
        String endpoint = "/api/bookings/42/supplier-payment-simulation";
        mvc.perform(get(endpoint).header("Authorization", "Bearer owner"))
            .andExpect(status().isNotFound());
        mvc.perform(post(endpoint).header("Authorization", "Bearer owner"))
            .andExpect(status().isNotFound());
        verifyNoInteractions(service);
    }
}
