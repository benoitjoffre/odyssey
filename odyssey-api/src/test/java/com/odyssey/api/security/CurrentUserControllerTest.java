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

        CurrentUserResponse response = currentUserController.getCurrentUser(jwt);

        verify(currentUserService).provisionUser(jwt);
        verify(currentUserService).getTravelerOnboardingStatus("auth0|traveler-a");
        assertEquals(List.of("TRAVELER"), response.roles());
        assertEquals(Boolean.FALSE, response.onboardingCompleted());
    }

    @Test
    void getCurrentUserDefaultsToTravelerWhenRolesClaimIsEmpty() {
        when(jwt.getClaimAsStringList(ROLES_CLAIM)).thenReturn(List.of());
        when(currentUserService.getTravelerOnboardingStatus("auth0|traveler-a"))
            .thenReturn(Boolean.FALSE);

        CurrentUserResponse response = currentUserController.getCurrentUser(jwt);

        assertEquals(List.of("TRAVELER"), response.roles());
        assertEquals(Boolean.FALSE, response.onboardingCompleted());
    }

    @Test
    void getCurrentUserReturnsTrueOnboardingStatusForCompletedTraveler() {
        when(jwt.getClaimAsStringList(ROLES_CLAIM)).thenReturn(List.of("TRAVELER"));
        when(currentUserService.getTravelerOnboardingStatus("auth0|traveler-a"))
            .thenReturn(Boolean.TRUE);

        CurrentUserResponse response = currentUserController.getCurrentUser(jwt);

        assertEquals(Boolean.TRUE, response.onboardingCompleted());
    }

    @Test
    void getCurrentUserReturnsNullOnboardingStatusWhenUserHasNoTraveler() {
        when(jwt.getClaimAsStringList(ROLES_CLAIM)).thenReturn(List.of("AGENT"));

        CurrentUserResponse response = currentUserController.getCurrentUser(jwt);

        assertEquals(List.of("AGENT"), response.roles());
        assertNull(response.onboardingCompleted());
        verify(currentUserService, never())
            .getTravelerOnboardingStatus("auth0|traveler-a");
    }

    @Test
    void getCurrentUserDefaultsTravelerOnboardingToFalseWhenTravelerRowMissing() {
        when(jwt.getClaimAsStringList(ROLES_CLAIM)).thenReturn(List.of("TRAVELER"));
        when(currentUserService.getTravelerOnboardingStatus("auth0|traveler-a"))
            .thenReturn(null);

        CurrentUserResponse response = currentUserController.getCurrentUser(jwt);

        assertEquals(List.of("TRAVELER"), response.roles());
        assertEquals(Boolean.FALSE, response.onboardingCompleted());
    }
}
