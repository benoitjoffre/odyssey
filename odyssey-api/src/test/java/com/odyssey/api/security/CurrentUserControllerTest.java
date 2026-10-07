package com.odyssey.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class CurrentUserControllerTest {

    private static final String ROLES_CLAIM = "https://odyssey.app/roles";

    private CurrentUserService currentUserService;
    private CurrentUserController currentUserController;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        currentUserService = mock(CurrentUserService.class);
        currentUserController = new CurrentUserController(currentUserService);
        jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("auth0|traveler-a");
    }

    @Test
    void getCurrentUserReturnsFalseOnboardingStatusForIncompleteTraveler() {
        when(jwt.getClaimAsStringList(ROLES_CLAIM)).thenReturn(List.of("TRAVELER"));
        when(currentUserService.getTravelerOnboardingStatus("auth0|traveler-a"))
            .thenReturn(Boolean.FALSE);
        when(currentUserService.getCurrentTravelerProfile("auth0|traveler-a"))
            .thenReturn(new CurrentTravelerProfile(
                "Alice",
                "Martin",
                "traveler@example.com",
                "+33600000000",
                "+33600000001",
                "fr"
            ));

        CurrentUserResponse response = currentUserController.getCurrentUser(jwt);

        verify(currentUserService).provisionUser(jwt);
        verify(currentUserService).getTravelerOnboardingStatus("auth0|traveler-a");
        verify(currentUserService).getCurrentTravelerProfile("auth0|traveler-a");
        assertEquals(List.of("TRAVELER"), response.roles());
        assertEquals(Boolean.FALSE, response.onboardingCompleted());
        assertEquals("Alice", response.firstName());
        assertEquals("Martin", response.lastName());
        assertEquals("traveler@example.com", response.email());
        assertEquals("+33600000000", response.phoneNumber());
        assertEquals("+33600000001", response.whatsappNumber());
        assertEquals("fr", response.preferredLanguage());
    }

    @Test
    void getCurrentUserDefaultsToTravelerWhenRolesClaimIsEmpty() {
        when(jwt.getClaimAsStringList(ROLES_CLAIM)).thenReturn(List.of());
        when(currentUserService.getTravelerOnboardingStatus("auth0|traveler-a"))
            .thenReturn(Boolean.FALSE);
        when(currentUserService.getCurrentTravelerProfile("auth0|traveler-a"))
            .thenReturn(new CurrentTravelerProfile(
                "Alice",
                "Martin",
                "traveler@example.com",
                "+33600000000",
                "+33600000001",
                "fr"
            ));

        CurrentUserResponse response = currentUserController.getCurrentUser(jwt);

        assertEquals(List.of("TRAVELER"), response.roles());
        assertEquals(Boolean.FALSE, response.onboardingCompleted());
        assertEquals("Alice", response.firstName());
        assertEquals("Martin", response.lastName());
        assertEquals("traveler@example.com", response.email());
        assertEquals("+33600000000", response.phoneNumber());
        assertEquals("+33600000001", response.whatsappNumber());
        assertEquals("fr", response.preferredLanguage());
    }

    @Test
    void getCurrentUserReturnsTrueOnboardingStatusForCompletedTraveler() {
        when(jwt.getClaimAsStringList(ROLES_CLAIM)).thenReturn(List.of("TRAVELER"));
        when(currentUserService.getTravelerOnboardingStatus("auth0|traveler-a"))
            .thenReturn(Boolean.TRUE);
        when(currentUserService.getCurrentTravelerProfile("auth0|traveler-a"))
            .thenReturn(new CurrentTravelerProfile(
                "Alice",
                "Martin",
                "traveler@example.com",
                "+33600000000",
                "+33600000001",
                "fr"
            ));

        CurrentUserResponse response = currentUserController.getCurrentUser(jwt);

        assertEquals(Boolean.TRUE, response.onboardingCompleted());
        assertEquals("traveler@example.com", response.email());
    }

    @Test
    void getCurrentUserReturnsNullOnboardingStatusWhenUserHasNoTraveler() {
        when(jwt.getClaimAsStringList(ROLES_CLAIM)).thenReturn(List.of("AGENT"));

        CurrentUserResponse response = currentUserController.getCurrentUser(jwt);

        assertEquals(List.of("AGENT"), response.roles());
        assertNull(response.onboardingCompleted());
        assertNull(response.firstName());
        assertNull(response.lastName());
        assertNull(response.email());
        assertNull(response.phoneNumber());
        assertNull(response.whatsappNumber());
        assertNull(response.preferredLanguage());
        verify(currentUserService, never())
            .getTravelerOnboardingStatus("auth0|traveler-a");
        verify(currentUserService, never())
            .getCurrentTravelerProfile("auth0|traveler-a");
    }

    @Test
    void getCurrentUserDefaultsTravelerOnboardingToFalseWhenTravelerRowMissing() {
        when(jwt.getClaimAsStringList(ROLES_CLAIM)).thenReturn(List.of("TRAVELER"));
        when(currentUserService.getTravelerOnboardingStatus("auth0|traveler-a"))
            .thenReturn(null);
        when(currentUserService.getCurrentTravelerProfile("auth0|traveler-a"))
            .thenReturn(null);

        CurrentUserResponse response = currentUserController.getCurrentUser(jwt);

        assertEquals(List.of("TRAVELER"), response.roles());
        assertEquals(Boolean.FALSE, response.onboardingCompleted());
        assertNull(response.email());
    }
}
