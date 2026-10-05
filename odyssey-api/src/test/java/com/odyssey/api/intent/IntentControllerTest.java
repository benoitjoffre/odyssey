package com.odyssey.api.intent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;

class IntentControllerTest {

    private IntentService intentService;
    private IntentController controller;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        intentService = mock(IntentService.class);
        controller = new IntentController(intentService);
        jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("auth0|traveler-a");
    }

    @Test
    void getIntentsForwardsSubjectAndIsTravelerOnly() throws Exception {
        controller.getIntents(jwt);

        verify(intentService).getIntents("auth0|traveler-a");
        assertTravelerOnly("getIntents", Jwt.class);
    }

    @Test
    void getIntentForwardsSubjectAndIsTravelerOnly() throws Exception {
        controller.getIntent(10L, jwt);

        verify(intentService).getIntent(10L, "auth0|traveler-a");
        assertTravelerOnly("getIntent", Long.class, Jwt.class);
    }

    @Test
    void getRecommendationsForwardsSubjectAndIsTravelerOnly()
        throws Exception {
        controller.getRecommendations(11L, jwt);

        verify(intentService).getRecommendations(11L, "auth0|traveler-a");
        assertTravelerOnly("getRecommendations", Long.class, Jwt.class);
    }

    private void assertTravelerOnly(String methodName, Class<?>... parameterTypes)
        throws Exception {
        Method method = IntentController.class.getMethod(methodName, parameterTypes);
        assertEquals(
            "hasRole('TRAVELER')",
            method.getAnnotation(PreAuthorize.class).value()
        );
    }
}
