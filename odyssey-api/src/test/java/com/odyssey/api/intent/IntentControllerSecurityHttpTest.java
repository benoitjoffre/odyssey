package com.odyssey.api.intent;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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

@SpringBootTest
@AutoConfigureMockMvc
class IntentControllerSecurityHttpTest {

    private static final String ROLES_CLAIM = "https://odyssey.app/roles";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IntentService intentService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getIntentsReturnsUnauthorizedWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/intents"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(intentService);
    }

    @Test
    void getIntentsReturnsForbiddenForAgentRole() throws Exception {
        when(jwtDecoder.decode("agent-token")).thenReturn(jwtWithRoles("AGENT"));

        mockMvc.perform(get("/api/intents")
                .header("Authorization", "Bearer agent-token"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(intentService);
    }

    @Test
    void getIntentReturnsForbiddenForAgentRole() throws Exception {
        when(jwtDecoder.decode("agent-token")).thenReturn(jwtWithRoles("AGENT"));

        mockMvc.perform(get("/api/intents/10")
                .header("Authorization", "Bearer agent-token"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(intentService);
    }

    @Test
    void getRecommendationsReturnsForbiddenForAgentRole() throws Exception {
        when(jwtDecoder.decode("agent-token")).thenReturn(jwtWithRoles("AGENT"));

        mockMvc.perform(get("/api/intents/10/recommendations")
                .header("Authorization", "Bearer agent-token"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(intentService);
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
