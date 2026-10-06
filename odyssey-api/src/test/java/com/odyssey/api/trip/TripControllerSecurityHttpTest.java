package com.odyssey.api.trip;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.odyssey.api.quote.QuoteService;

@SpringBootTest
@AutoConfigureMockMvc
class TripControllerSecurityHttpTest {

    private static final String ROLES_CLAIM = "https://odyssey.app/roles";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TripService tripService;

    @MockitoBean
    private QuoteService quoteService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getTripReturnsUnauthorizedWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/trips/10"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(tripService);
    }

    @Test
    void getTripReturnsForbiddenForAgentRole() throws Exception {
        when(jwtDecoder.decode("agent-token"))
            .thenReturn(jwtWithRoles("AGENT"));

        mockMvc.perform(get("/api/trips/10")
                .header("Authorization", "Bearer agent-token"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(tripService);
    }

    @Test
    void getTripAllowsTravelerRole() throws Exception {
        when(jwtDecoder.decode("traveler-token"))
            .thenReturn(jwtWithRoles("TRAVELER"));
        when(tripService.getTrip(10L, "auth0|subject"))
            .thenReturn(new TripResponse(
                10L,
                "Trip",
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(12),
                TripStatus.DRAFT,
                1L,
                null,
                BigDecimal.ZERO
            ));

        mockMvc.perform(get("/api/trips/10")
                .header("Authorization", "Bearer traveler-token"))
            .andExpect(status().isOk());

        verify(tripService).getTrip(10L, "auth0|subject");
    }

    private Jwt jwtWithRoles(String... roles) {
        Instant now = Instant.now();

        return new Jwt(
            "token-value",
            now,
            now.plusSeconds(3600),
            Map.of("alg", "none"),
            Map.of(
                "sub", "auth0|subject",
                ROLES_CLAIM, List.of(roles)
            )
        );
    }
}
