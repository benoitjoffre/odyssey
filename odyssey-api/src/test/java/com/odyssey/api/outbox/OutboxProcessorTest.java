package com.odyssey.api.outbox;

import com.odyssey.api.event.BookingRequestedEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxProcessorTest {

    private OutboxEventRepository outboxEventRepository;
    private ApplicationEventPublisher applicationEventPublisher;
    private OutboxProcessor outboxProcessor;

    @BeforeEach
    void setUp() {
        outboxEventRepository = mock(OutboxEventRepository.class);
        applicationEventPublisher = mock(ApplicationEventPublisher.class);
        outboxProcessor = new OutboxProcessor(
            outboxEventRepository,
            applicationEventPublisher,
            new ObjectMapper()
        );
    }

    private OutboxEvent pendingBookingRequestedEvent(Long id) {
        OutboxEvent event = mock(OutboxEvent.class);
        when(event.getId()).thenReturn(id);
        when(event.getEventType()).thenReturn("BOOKING_REQUESTED");
        when(event.getPayload()).thenReturn(
            new ObjectMapper().writeValueAsString(
                new BookingRequestedEvent(10L, 20L, 30L)
            )
        );
        when(event.getStatus()).thenReturn(OutboxStatus.PENDING);
        when(event.getCreatedAt()).thenReturn(Instant.now());
        return event;
    }

    @Test
    void publishesDeserializedEventAndMarksProcessed() {

        Long eventId = 1L;
        OutboxEvent event = pendingBookingRequestedEvent(eventId);

        when(outboxEventRepository.findByStatus(OutboxStatus.PENDING))
            .thenReturn(List.of(event));
        when(outboxEventRepository.updateStatusIfCurrent(
            eq(eventId), eq(OutboxStatus.PENDING), eq(OutboxStatus.PROCESSING)
        )).thenReturn(1);
        when(outboxEventRepository.findById(eventId))
            .thenReturn(Optional.of(event));

        outboxProcessor.processPendingEvents();

        verify(applicationEventPublisher).publishEvent(
            new BookingRequestedEvent(10L, 20L, 30L)
        );
        verify(outboxEventRepository).updateStatusIfCurrent(
            eq(eventId), eq(OutboxStatus.PROCESSING), eq(OutboxStatus.PROCESSED)
        );
    }

    @Test
    void skipsEventAlreadyClaimedByAnotherRun() {

        Long eventId = 2L;
        OutboxEvent event = pendingBookingRequestedEvent(eventId);

        when(outboxEventRepository.findByStatus(OutboxStatus.PENDING))
            .thenReturn(List.of(event));
        when(outboxEventRepository.updateStatusIfCurrent(
            any(), eq(OutboxStatus.PENDING), eq(OutboxStatus.PROCESSING)
        )).thenReturn(0);

        outboxProcessor.processPendingEvents();

        verify(applicationEventPublisher, never()).publishEvent(any(Object.class));
        verify(outboxEventRepository, never()).findById(any());
    }

    @Test
    void revertsToPendingWhenPublishingFails() {

        Long eventId = 3L;
        OutboxEvent event = pendingBookingRequestedEvent(eventId);

        when(outboxEventRepository.findByStatus(OutboxStatus.PENDING))
            .thenReturn(List.of(event));
        when(outboxEventRepository.updateStatusIfCurrent(
            any(), eq(OutboxStatus.PENDING), eq(OutboxStatus.PROCESSING)
        )).thenReturn(1);
        when(outboxEventRepository.findById(eventId))
            .thenReturn(Optional.of(event));

        doThrow(new RuntimeException("listener failure"))
            .when(applicationEventPublisher).publishEvent(any(Object.class));

        outboxProcessor.processPendingEvents();

        verify(outboxEventRepository).updateStatusIfCurrent(
            eq(eventId), eq(OutboxStatus.PROCESSING), eq(OutboxStatus.PENDING)
        );
        verify(outboxEventRepository, never()).updateStatusIfCurrent(
            eq(eventId), eq(OutboxStatus.PROCESSING), eq(OutboxStatus.PROCESSED)
        );
    }
}
