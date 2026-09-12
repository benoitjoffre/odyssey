package com.odyssey.api.trip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ResponseStatus;

class TripControllerTest {

    private TripService tripService;
    private TripController tripController;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        tripService = mock(TripService.class);
        tripController = new TripController(tripService);
        jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("auth0|traveler-a");
    }

    @Test
    void deleteTripForwardsAuthenticatedSubjectAndKeepsNoContentStatus()
        throws Exception {
        tripController.deleteTrip(10L, jwt);

        verify(tripService).deleteTrip(10L, "auth0|traveler-a");

        Method method = TripController.class.getMethod(
            "deleteTrip",
            Long.class,
            Jwt.class
        );
        assertEquals(
            HttpStatus.NO_CONTENT,
            method.getAnnotation(ResponseStatus.class).value()
        );
    }

    @Test
    void getTripDetailForwardsAuthenticatedSubject() {
        tripController.getTripDetail(10L, jwt);

        verify(tripService).getTripDetail(10L, "auth0|traveler-a");
    }
}