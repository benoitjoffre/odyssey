package com.odyssey.api.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerNotification;
import com.odyssey.api.traveler.TravelerNotificationRepository;
import com.odyssey.api.traveler.TravelerNotificationResponse;
import com.odyssey.api.traveler.TravelerNotificationSseService;
import com.odyssey.api.traveler.TravelerRepository;

class TravelerNotificationEventListenerTest {

    private TravelerRepository travelerRepository;
    private TravelerNotificationRepository notificationRepository;
    private TravelerNotificationSseService sseService;
    private TravelerNotificationEventListener listener;

    @BeforeEach
    void setUp() {
        travelerRepository = mock(TravelerRepository.class);
        notificationRepository = mock(TravelerNotificationRepository.class);
        sseService = mock(TravelerNotificationSseService.class);
        listener = new TravelerNotificationEventListener(
            travelerRepository,
            notificationRepository,
            sseService
        );
    }

    @Test
    void tripQuotesSentCreatesNotificationAndPushesSse() {
        Traveler traveler = new Traveler();
        traveler.setId(42L);
        traveler.setFirstName("Alice");
        traveler.setLastName("Martin");
        when(travelerRepository.findById(42L)).thenReturn(Optional.of(traveler));

        when(notificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        listener.onTripQuotesSent(new TripQuotesSentEvent(99L, 42L, List.of(10L, 11L)));

        verify(notificationRepository).save(any(TravelerNotification.class));
        verify(sseService).send(eq(42L), any(TravelerNotificationResponse.class));
    }

    @Test
    void tripQuotesSentCreatesUnreadNotificationWithTravelerTargetUrl() {
        Traveler traveler = new Traveler();
        traveler.setId(42L);
        traveler.setFirstName("Alice");
        traveler.setLastName("Martin");
        when(travelerRepository.findById(42L)).thenReturn(Optional.of(traveler));

        when(notificationRepository.save(any(TravelerNotification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        listener.onTripQuotesSent(new TripQuotesSentEvent(99L, 42L, List.of(10L)));

        var notificationArg = org.mockito.Mockito.mockingDetails(notificationRepository)
            .getInvocations()
            .stream()
            .filter(invocation -> invocation.getMethod().getName().equals("save"))
            .map(invocation -> invocation.getArgument(0, TravelerNotification.class))
            .findFirst()
            .orElseThrow();

        assertEquals("Nouvelle proposition disponible", notificationArg.getTitle());
        assertEquals("Votre conseiller a préparé une nouvelle proposition pour votre voyage.", notificationArg.getMessage());
        assertEquals("/traveler/quotes", notificationArg.getTargetUrl());
        assertFalse(notificationArg.isRead());
        assertEquals(42L, notificationArg.getTraveler().getId());
    }
}
