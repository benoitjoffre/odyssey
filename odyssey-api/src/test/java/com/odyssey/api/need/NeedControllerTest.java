package com.odyssey.api.need;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;

class NeedControllerTest {

    private NeedService needService;
    private NeedController controller;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        needService = mock(NeedService.class);
        controller = new NeedController(needService);
        jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("auth0|traveler-a");
    }

    @Test
    void createNeedForwardsAuthenticatedSubjectAndIsTravelerOnly()
        throws Exception {
        CreateNeedRequest request = mock(CreateNeedRequest.class);

        controller.createNeed(request, jwt);

        verify(needService).createNeed(request, "auth0|traveler-a");
        assertTravelerOnly("createNeed", CreateNeedRequest.class, Jwt.class);
    }

    @Test
    void getNeedsForwardsAuthenticatedSubjectAndIsTravelerOnly()
        throws Exception {
        controller.getNeeds(jwt);

        verify(needService).getNeeds("auth0|traveler-a");
        assertTravelerOnly("getNeeds", Jwt.class);
    }

    @Test
    void getNeedForwardsAuthenticatedSubjectAndIsTravelerOnly()
        throws Exception {
        controller.getNeed(11L, jwt);

        verify(needService).getNeed(11L, "auth0|traveler-a");
        assertTravelerOnly("getNeed", Long.class, Jwt.class);
    }

    @Test
    void getNeedsByTripForwardsAuthenticatedSubjectAndIsTravelerOnly()
        throws Exception {
        controller.getNeedsByTrip(12L, jwt);

        verify(needService).getNeedsByTrip(12L, "auth0|traveler-a");
        assertTravelerOnly("getNeedsByTrip", Long.class, Jwt.class);
    }

    @Test
    void updateNeedNotesForwardsAuthenticatedSubjectAndIsTravelerOnly()
        throws Exception {
        UpdateNeedNotesRequest request = new UpdateNeedNotesRequest();
        request.setNotes("notes");

        controller.updateNeedNotes(13L, request, jwt);

        verify(needService).updateNotes(13L, request, "auth0|traveler-a");
        assertTravelerOnly(
            "updateNeedNotes",
            Long.class,
            UpdateNeedNotesRequest.class,
            Jwt.class
        );
    }

    private void assertTravelerOnly(String methodName, Class<?>... parameterTypes)
        throws Exception {
        Method method = NeedController.class.getMethod(methodName, parameterTypes);
        assertEquals(
            "hasRole('TRAVELER')",
            method.getAnnotation(PreAuthorize.class).value()
        );
    }
}
