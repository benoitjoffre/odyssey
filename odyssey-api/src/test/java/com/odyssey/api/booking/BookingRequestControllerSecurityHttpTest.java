package com.odyssey.api.booking;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.odyssey.api.quote.QuoteService;

@SpringBootTest
@AutoConfigureMockMvc
class BookingRequestControllerSecurityHttpTest {

    private static final String ROLES_CLAIM = "https://odyssey.app/roles";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingRequestService bookingRequestService;

    @MockitoBean
    private QuoteService quoteService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getBookingRequestsReturnsUnauthorizedWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/booking-requests"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(bookingRequestService);
        verifyNoInteractions(quoteService);
    }

    @Test
    void getBookingRequestsReturnsForbiddenForTravelerRole() throws Exception {
        when(jwtDecoder.decode("traveler-token"))
            .thenReturn(jwtWithRoles("TRAVELER"));

        mockMvc.perform(get("/api/booking-requests")
                .header("Authorization", "Bearer traveler-token"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(bookingRequestService);
        verifyNoInteractions(quoteService);
    }

    @Test
    void createBookingRequestReturnsForbiddenForAgentRole() throws Exception {
        when(jwtDecoder.decode("agent-token")).thenReturn(jwtWithRoles("AGENT"));

        mockMvc.perform(post("/api/booking-requests")
                .header("Authorization", "Bearer agent-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validCreateBookingRequestPayload()))
            .andExpect(status().isForbidden());

        verifyNoInteractions(bookingRequestService);
        verifyNoInteractions(quoteService);
    }

    private String validCreateBookingRequestPayload() {
        return """
            {
              "needId": 10,
              "notes": "Please find a family-friendly offer"
            }
            """;
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
