package com.odyssey.api.traveler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class TravelerControllerTest {

    private TravelerService travelerService;
    private TravelerController travelerController;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        travelerService = mock(TravelerService.class);
        travelerController = new TravelerController(travelerService);
        jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("auth0|traveler-a");
    }

    @Test
    void completeOnboardingForwardsAuthenticatedSubjectAndReturnsServiceResult() {
        TravelerOnboardingRequest request = new TravelerOnboardingRequest(
            "Alice",
            "Martin",
            "+33600000000",
            "+33600000001",
            "fr"
        );
        Traveler expected = new Traveler("Alice", "Martin", "traveler@example.com");
        when(travelerService.completeOnboarding("auth0|traveler-a", request))
            .thenReturn(expected);

        Traveler result = travelerController.completeOnboarding(jwt, request);

        assertSame(expected, result);
        verify(travelerService).completeOnboarding("auth0|traveler-a", request);
    }

    @Test
    void completeOnboardingMethodNeverAcceptsAClientSuppliedIdentifier()
        throws Exception {
        Method method = TravelerController.class.getMethod(
            "completeOnboarding",
            Jwt.class,
            TravelerOnboardingRequest.class
        );

        assertEquals(2, method.getParameterCount());
        assertTrue(
            Arrays.stream(method.getParameterTypes())
                .noneMatch(type -> type == Long.class || type == long.class),
            "completeOnboarding must not accept an id/travelerId parameter from the client"
        );
    }

    @Test
    void onboardingRequestNeverExposesATravelerIdOrAuth0SubjectComponent() {
        RecordComponent[] components =
            TravelerOnboardingRequest.class.getRecordComponents();

        assertTrue(
            Arrays.stream(components)
                .map(RecordComponent::getName)
                .noneMatch(name -> name.equalsIgnoreCase("travelerId")
                    || name.equalsIgnoreCase("auth0Subject")
                    || name.equalsIgnoreCase("id")),
            "TravelerOnboardingRequest must not allow the client to supply an identifier"
        );
    }
}
