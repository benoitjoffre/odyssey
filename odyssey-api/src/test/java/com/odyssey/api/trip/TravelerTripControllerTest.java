package com.odyssey.api.trip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;

class TravelerTripControllerTest {

    private TripService tripService;
    private TravelerTripController controller;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        tripService = mock(TripService.class);
        controller = new TravelerTripController(tripService);
        jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("auth0|traveler-a");
    }

    @Test
    void getTravelerTripsForwardsSubjectAndIsTravelerOnly() throws Exception {
        controller.getTravelerTrips(5L, jwt);

        verify(tripService).getOwnedTripsByTravelerId(5L, "auth0|traveler-a");

        Method method = TravelerTripController.class.getMethod(
            "getTravelerTrips",
            Long.class,
            Jwt.class
        );
        assertEquals(
            "hasRole('TRAVELER')",
            method.getAnnotation(PreAuthorize.class).value()
        );
    }
}
