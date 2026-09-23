package com.odyssey.api.traveler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import com.odyssey.api.event.TravelerOnboardingCompletedEvent;
import com.odyssey.api.outbox.OutboxEvent;
import com.odyssey.api.outbox.OutboxEventRepository;
import com.odyssey.api.outbox.OutboxStatus;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class TravelerServiceTest {

    private static final String AUTH0_SUBJECT = "auth0|traveler-1";

    @Mock
    private TravelerRepository travelerRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    private TravelerService travelerService;

    @BeforeEach
    void setUp() {
        travelerService = new TravelerService(
            travelerRepository,
            outboxEventRepository,
            objectMapper
        );
    }

    @Test
    void completeOnboardingFindsTravelerByAuth0SubjectUpdatesFieldsAndSaves() {
        Traveler traveler = new Traveler(
            "OldFirst",
            "OldLast",
            "traveler@example.com"
        );
        traveler.setAuth0Subject(AUTH0_SUBJECT);
        TravelerOnboardingRequest request = new TravelerOnboardingRequest(
            "Alice",
            "Martin",
            "+33600000000",
            "+33600000001",
            "fr"
        );
        when(travelerRepository.findByAuth0Subject(AUTH0_SUBJECT))
            .thenReturn(Optional.of(traveler));
        when(travelerRepository.save(traveler)).thenReturn(traveler);
        when(objectMapper.writeValueAsString(any(TravelerOnboardingCompletedEvent.class)))
            .thenReturn("{\"travelerId\":1}");
        when(outboxEventRepository.save(any(OutboxEvent.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Traveler result = travelerService.completeOnboarding(
            AUTH0_SUBJECT,
            request
        );

        assertSame(traveler, result);
        assertEquals("Alice", result.getFirstName());
        assertEquals("Martin", result.getLastName());
        assertEquals("+33600000000", result.getPhoneNumber());
        assertEquals("+33600000001", result.getWhatsappNumber());
        assertEquals("fr", result.getPreferredLanguage());
        assertTrue(result.isOnboardingCompleted());
        verify(travelerRepository).save(traveler);

        ArgumentCaptor<OutboxEvent> outboxEventCaptor =
            ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxEventCaptor.capture());

        OutboxEvent outboxEvent = outboxEventCaptor.getValue();
        assertEquals("TRAVELER_ONBOARDING_COMPLETED", outboxEvent.getEventType());
        assertEquals("{\"travelerId\":1}", outboxEvent.getPayload());
        assertEquals(OutboxStatus.PENDING, outboxEvent.getStatus());
        assertTrue(outboxEvent.getCreatedAt().isBefore(Instant.now().plusSeconds(1)));
    }

    @Test
    void completeOnboardingThrowsWhenTravelerDoesNotExist() {
        TravelerOnboardingRequest request = new TravelerOnboardingRequest(
            "Alice",
            "Martin",
            "+33600000000",
            null,
            "fr"
        );
        when(travelerRepository.findByAuth0Subject(AUTH0_SUBJECT))
            .thenReturn(Optional.empty());

        assertThrows(
            IllegalArgumentException.class,
            () -> travelerService.completeOnboarding(AUTH0_SUBJECT, request)
        );

        verify(travelerRepository, never()).save(any());
    }
}
