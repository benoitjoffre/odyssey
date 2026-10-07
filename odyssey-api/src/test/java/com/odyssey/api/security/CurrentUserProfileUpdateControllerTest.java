package com.odyssey.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;

class CurrentUserProfileUpdateControllerTest {

    private static final String SUBJECT = "auth0|traveler-a";
    private static final String ROLES_CLAIM = "https://odyssey.app/roles";

    private CurrentUserService currentUserService;
    private CurrentUserController currentUserController;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        currentUserService = mock(CurrentUserService.class);
        currentUserController = new CurrentUserController(currentUserService);
        jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(SUBJECT);
        when(jwt.getClaimAsStringList(ROLES_CLAIM)).thenReturn(List.of("TRAVELER"));
    }

    @Test
    void patchMeUsesAuthenticatedSubjectAndReturnsUpdatedProfile() {
        UpdateProfileRequest request = new UpdateProfileRequest(
            "Alice",
            "Martin",
            "+33600000000",
            null,
            "fr"
        );
        CurrentTravelerProfile updatedProfile = new CurrentTravelerProfile(
            "Alice",
            "Martin",
            "traveler@example.com",
            "+33600000000",
            null,
            "fr"
        );
        when(currentUserService.updateCurrentTravelerProfile(SUBJECT, request))
            .thenReturn(updatedProfile);
        when(currentUserService.getTravelerOnboardingStatus(SUBJECT))
            .thenReturn(Boolean.TRUE);

        CurrentUserResponse response = currentUserController
            .updateCurrentTravelerProfile(jwt, request);

        verify(currentUserService).provisionUser(jwt);
        verify(currentUserService).updateCurrentTravelerProfile(SUBJECT, request);
        verify(currentUserService).getTravelerOnboardingStatus(SUBJECT);
        assertEquals(List.of("TRAVELER"), response.roles());
        assertEquals(Boolean.TRUE, response.onboardingCompleted());
        assertEquals("Alice", response.firstName());
        assertEquals("Martin", response.lastName());
        assertEquals("traveler@example.com", response.email());
        assertEquals("+33600000000", response.phoneNumber());
        assertSame(null, response.whatsappNumber());
        assertEquals("fr", response.preferredLanguage());
    }

    @Test
    void patchMeIsTravelerOnly() throws Exception {
        Method method = CurrentUserController.class.getMethod(
            "updateCurrentTravelerProfile",
            Jwt.class,
            UpdateProfileRequest.class
        );

        assertEquals(
            "hasRole('TRAVELER')",
            method.getAnnotation(PreAuthorize.class).value()
        );
    }

    @Test
    void patchMeMethodNeverAcceptsAClientSuppliedIdentifier() throws Exception {
        Method method = CurrentUserController.class.getMethod(
            "updateCurrentTravelerProfile",
            Jwt.class,
            UpdateProfileRequest.class
        );

        assertEquals(2, method.getParameterCount());
        assertTrue(
            Arrays.stream(method.getParameterTypes())
                .noneMatch(type -> type == Long.class || type == long.class),
            "PATCH /api/me must not accept an id/travelerId parameter from the client"
        );
    }

    @Test
    void updateProfileRequestNeverExposesProtectedFields() {
        RecordComponent[] components =
            UpdateProfileRequest.class.getRecordComponents();

        assertTrue(
            Arrays.stream(components)
                .map(RecordComponent::getName)
                .noneMatch(name -> name.equalsIgnoreCase("travelerId")
                    || name.equalsIgnoreCase("auth0Subject")
                    || name.equalsIgnoreCase("id")
                    || name.equalsIgnoreCase("email")
                    || name.equalsIgnoreCase("roles")
                    || name.equalsIgnoreCase("onboardingCompleted")),
            "UpdateProfileRequest must not allow protected fields"
        );
    }

    @Test
    void patchMeNeverReadsTravelerProfileForAgentRole() {
        when(jwt.getClaimAsStringList(ROLES_CLAIM)).thenReturn(List.of("AGENT"));
        UpdateProfileRequest request = new UpdateProfileRequest(
            "Alice",
            "Martin",
            "+33600000000",
            null,
            "fr"
        );
        when(currentUserService.updateCurrentTravelerProfile(SUBJECT, request))
            .thenReturn(new CurrentTravelerProfile(
                "Alice",
                "Martin",
                "traveler@example.com",
                "+33600000000",
                null,
                "fr"
            ));
        when(currentUserService.getTravelerOnboardingStatus(SUBJECT)).thenReturn(Boolean.TRUE);

        currentUserController.updateCurrentTravelerProfile(jwt, request);

        verify(currentUserService, never()).getCurrentTravelerProfile(SUBJECT);
    }
}