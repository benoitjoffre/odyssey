package com.odyssey.api.traveler;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class TravelerOnboardingSecurityHttpTest {

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
    void onboardingReturnsUnauthorizedWithoutJwt() throws Exception {
        mockMvc.perform(put("/api/travelers/me/onboarding")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOnboardingPayload()))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(travelerService);
    }

    @Test
    void onboardingReturnsForbiddenForAgentRole() throws Exception {
        when(jwtDecoder.decode("agent-token")).thenReturn(jwtWithRoles("AGENT"));

        mockMvc.perform(put("/api/travelers/me/onboarding")
                .header("Authorization", "Bearer agent-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOnboardingPayload()))
            .andExpect(status().isForbidden());

        verifyNoInteractions(travelerService);
    }

    private String validOnboardingPayload() {
        return """
            {
              "firstName": "Alice",
              "lastName": "Martin",
                            "phoneNumber": "+33600000000",
                            "whatsappNumber": "+33600000001",
              "preferredLanguage": "fr"
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
