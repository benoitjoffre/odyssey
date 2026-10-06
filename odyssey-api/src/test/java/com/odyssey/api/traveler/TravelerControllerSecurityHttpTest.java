package com.odyssey.api.traveler;

import static org.mockito.Mockito.verify;
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

@SpringBootTest
@AutoConfigureMockMvc
class TravelerControllerSecurityHttpTest {

    private static final String ROLES_CLAIM = "https://odyssey.app/roles";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TravelerService travelerService;

    @MockitoBean
    private TravelerNotificationSseService sseService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getTravelersReturnsUnauthorizedWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/travelers"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(travelerService);
    }

    @Test
    void getTravelersReturnsForbiddenForTravelerRole() throws Exception {
        when(jwtDecoder.decode("traveler-token"))
            .thenReturn(jwtWithRoles("TRAVELER"));

        mockMvc.perform(get("/api/travelers")
                .header("Authorization", "Bearer traveler-token"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(travelerService);
    }

    @Test
    void createTravelerReturnsForbiddenForTravelerRole() throws Exception {
        when(jwtDecoder.decode("traveler-token"))
            .thenReturn(jwtWithRoles("TRAVELER"));

        mockMvc.perform(post("/api/travelers")
                .header("Authorization", "Bearer traveler-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validTravelerPayload()))
            .andExpect(status().isForbidden());

        verifyNoInteractions(travelerService);
    }

    @Test
    void getTravelersAllowsAgentRole() throws Exception {
        when(jwtDecoder.decode("agent-token"))
            .thenReturn(jwtWithRoles("AGENT"));
        when(travelerService.getTravelers()).thenReturn(List.of());

        mockMvc.perform(get("/api/travelers")
                .header("Authorization", "Bearer agent-token"))
            .andExpect(status().isOk());

        verify(travelerService).getTravelers();
    }

    private String validTravelerPayload() {
        return """
            {
              "firstName": "Alice",
              "lastName": "Martin",
              "email": "alice@example.com"
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
