package com.odyssey.api.need;

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
class NeedControllerSecurityHttpTest {

    private static final String ROLES_CLAIM = "https://odyssey.app/roles";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NeedService needService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getNeedsReturnsUnauthorizedWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/needs"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(needService);
    }

    @Test
    void getNeedsReturnsForbiddenForAgentRole() throws Exception {
        when(jwtDecoder.decode("agent-token")).thenReturn(jwtWithRoles("AGENT"));

        mockMvc.perform(get("/api/needs")
                .header("Authorization", "Bearer agent-token"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(needService);
    }

    @Test
    void getNeedReturnsForbiddenForAgentRole() throws Exception {
        when(jwtDecoder.decode("agent-token")).thenReturn(jwtWithRoles("AGENT"));

        mockMvc.perform(get("/api/needs/10")
                .header("Authorization", "Bearer agent-token"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(needService);
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
