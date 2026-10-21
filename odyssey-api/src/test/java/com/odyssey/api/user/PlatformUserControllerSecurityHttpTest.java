package com.odyssey.api.user;

import com.odyssey.api.security.SecurityConfig;
import com.odyssey.api.user.PlatformUserResponse.ProfileType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlatformUserController.class)
@Import(SecurityConfig.class)
class PlatformUserControllerSecurityHttpTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PlatformUserService service;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void anonymousCannotReadDirectory() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void travelerCannotReadDirectory() throws Exception {
        authenticate("TRAVELER");
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer directory-test"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void agentCanReadDirectoryWithoutAuthenticationIdentifiers() throws Exception {
        authenticate("AGENT");
        var user = new PlatformUserResponse(
            1L, ProfileType.TRAVELER, "Marie", "Martin", "marie@example.com",
            "+33612345678", null, "fr", true, null
        );
        when(service.getUsers(ProfileType.TRAVELER, "", 0, 20))
            .thenReturn(new PlatformUserService.UserPage(List.of(user), 0, 20, 1, 1));

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer directory-test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].email").value("marie@example.com"))
            .andExpect(jsonPath("$.items[0].phoneNumber").value("+33612345678"))
            .andExpect(jsonPath("$.items[0].onboardingCompleted").value(true))
            .andExpect(jsonPath("$.items[0].auth0Subject").doesNotExist())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void passesAgentSearchAndPageToService() throws Exception {
        authenticate("AGENT");
        when(service.getUsers(ProfileType.AGENT, "Martin", 2, 10))
            .thenReturn(new PlatformUserService.UserPage(List.of(), 2, 10, 0, 0));
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer directory-test")
                .param("profileType", "AGENT").param("search", "Martin")
                .param("page", "2").param("size", "10"))
            .andExpect(status().isOk());
        verify(service).getUsers(ProfileType.AGENT, "Martin", 2, 10);
    }

    @Test
    void rejectsUnknownProfileType() throws Exception {
        authenticate("AGENT");
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer directory-test").param("profileType", "ADMIN"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void returnsBadRequestForInvalidPagination() throws Exception {
        authenticate("AGENT");
        when(service.getUsers(ProfileType.TRAVELER, "", -1, 20))
            .thenThrow(new IllegalArgumentException("Invalid page"));
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer directory-test").param("page", "-1"))
            .andExpect(status().isBadRequest());
    }

    private void authenticate(String role) {
        Instant now = Instant.now();
        when(jwtDecoder.decode("directory-test")).thenReturn(new Jwt(
            "directory-test", now, now.plusSeconds(3600), Map.of("alg", "none"),
            Map.of("sub", "auth0|directory-test", "https://odyssey.app/roles", List.of(role))
        ));
    }
}
