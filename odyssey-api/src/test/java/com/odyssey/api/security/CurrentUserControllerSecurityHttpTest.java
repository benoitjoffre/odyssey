package com.odyssey.api.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.hamcrest.Matchers.nullValue;

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
class CurrentUserControllerSecurityHttpTest {

    private static final String ROLES_CLAIM = "https://odyssey.app/roles";
    private static final String SUBJECT = "auth0|subject";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getMeReturnsUnauthorizedWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/me"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(currentUserService);
    }

    @Test
    void getMeReturnsTravelerProfileForTravelerRole() throws Exception {
        when(jwtDecoder.decode("traveler-token")).thenReturn(jwtWithRoles("TRAVELER"));
        when(currentUserService.getTravelerOnboardingStatus(SUBJECT))
            .thenReturn(Boolean.TRUE);
        when(currentUserService.getCurrentTravelerProfile(SUBJECT))
            .thenReturn(new CurrentTravelerProfile(
                "Alice",
                "Martin",
                "traveler@example.com",
                "+33600000000",
                "+33600000001",
                "fr"
            ));

        mockMvc.perform(get("/api/me")
                .header("Authorization", "Bearer traveler-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roles[0]").value("TRAVELER"))
            .andExpect(jsonPath("$.onboardingCompleted").value(true))
            .andExpect(jsonPath("$.firstName").value("Alice"))
            .andExpect(jsonPath("$.lastName").value("Martin"))
            .andExpect(jsonPath("$.email").value("traveler@example.com"))
            .andExpect(jsonPath("$.phoneNumber").value("+33600000000"))
            .andExpect(jsonPath("$.whatsappNumber").value("+33600000001"))
            .andExpect(jsonPath("$.preferredLanguage").value("fr"));

        verify(currentUserService).provisionUser(any());
        verify(currentUserService).getCurrentTravelerProfile(SUBJECT);
    }

    @Test
    void getMeStillWorksForAgentWithoutTravelerFields() throws Exception {
        when(jwtDecoder.decode("agent-token")).thenReturn(jwtWithRoles("AGENT"));

        mockMvc.perform(get("/api/me")
                .header("Authorization", "Bearer agent-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roles[0]").value("AGENT"))
            .andExpect(jsonPath("$.onboardingCompleted").value(nullValue()))
            .andExpect(jsonPath("$.firstName").value(nullValue()))
            .andExpect(jsonPath("$.lastName").value(nullValue()))
            .andExpect(jsonPath("$.email").value(nullValue()))
            .andExpect(jsonPath("$.phoneNumber").value(nullValue()))
            .andExpect(jsonPath("$.whatsappNumber").value(nullValue()))
            .andExpect(jsonPath("$.preferredLanguage").value(nullValue()));

        verify(currentUserService).provisionUser(any());
        verify(currentUserService, never()).getTravelerOnboardingStatus(SUBJECT);
        verify(currentUserService, never()).getCurrentTravelerProfile(SUBJECT);
    }

    @Test
    void patchMeReturnsUnauthorizedWithoutJwt() throws Exception {
        mockMvc.perform(patch("/api/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validPatchPayload()))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(currentUserService);
    }

    @Test
    void patchMeReturnsForbiddenForAgentRole() throws Exception {
        when(jwtDecoder.decode("agent-token")).thenReturn(jwtWithRoles("AGENT"));

        mockMvc.perform(patch("/api/me")
                .header("Authorization", "Bearer agent-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validPatchPayload()))
            .andExpect(status().isForbidden());

        verifyNoInteractions(currentUserService);
    }

    @Test
    void patchMeUsesJwtSubjectToUpdateCurrentTravelerProfile() throws Exception {
        when(jwtDecoder.decode("traveler-token")).thenReturn(jwtWithRoles("TRAVELER"));
        when(currentUserService.updateCurrentTravelerProfile(any(), any()))
            .thenReturn(new CurrentTravelerProfile(
                "Alice",
                "Martin",
                "traveler@example.com",
                "+33600000000",
                null,
                "fr"
            ));
        when(currentUserService.getTravelerOnboardingStatus(SUBJECT))
            .thenReturn(Boolean.TRUE);

        mockMvc.perform(patch("/api/me")
                .header("Authorization", "Bearer traveler-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validPatchPayload()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("traveler@example.com"));

        verify(currentUserService).updateCurrentTravelerProfile(eq(SUBJECT), any());
    }

    @Test
    void patchMeRejectsInvalidPayload() throws Exception {
        when(jwtDecoder.decode("traveler-token")).thenReturn(jwtWithRoles("TRAVELER"));

        mockMvc.perform(patch("/api/me")
                .header("Authorization", "Bearer traveler-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPatchPayload()))
            .andExpect(status().isBadRequest());

        verify(currentUserService, never())
            .updateCurrentTravelerProfile(any(), any());
    }

    @Test
    void patchMeAcceptsEmptyWhatsappNumber() throws Exception {
        when(jwtDecoder.decode("traveler-token")).thenReturn(jwtWithRoles("TRAVELER"));
        when(currentUserService.updateCurrentTravelerProfile(any(), any()))
            .thenReturn(new CurrentTravelerProfile(
                "Alice",
                "Martin",
                "traveler@example.com",
                "+33600000000",
                "",
                "fr"
            ));
        when(currentUserService.getTravelerOnboardingStatus(SUBJECT))
            .thenReturn(Boolean.TRUE);

        mockMvc.perform(patch("/api/me")
                .header("Authorization", "Bearer traveler-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validPatchPayloadWithEmptyWhatsapp()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.whatsappNumber").value(""));
    }

    private String validPatchPayload() {
        return """
            {
              "firstName": "Alice",
              "lastName": "Martin",
              "phoneNumber": "+33600000000",
              "whatsappNumber": null,
              "preferredLanguage": "fr"
            }
            """;
    }

    private String invalidPatchPayload() {
        return """
            {
              "firstName": " ",
              "lastName": "Martin",
              "phoneNumber": "+33600000000",
              "preferredLanguage": "fr"
            }
            """;
    }

    private String validPatchPayloadWithEmptyWhatsapp() {
        return """
            {
              "firstName": "Alice",
              "lastName": "Martin",
              "phoneNumber": "+33600000000",
              "whatsappNumber": "",
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
                "sub", SUBJECT,
                ROLES_CLAIM, List.of(roles)
            )
        );
    }
}