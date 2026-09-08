package com.odyssey.api.event;

import com.odyssey.api.agent.Agent;
import com.odyssey.api.agent.AgentNotification;
import com.odyssey.api.agent.AgentNotificationRepository;
import com.odyssey.api.agent.AgentNotificationResponse;
import com.odyssey.api.agent.AgentNotificationSseService;
import com.odyssey.api.agent.AgentRepository;
import com.odyssey.api.agent.AgentStatus;
import com.odyssey.api.booking.BookingRequest;
import com.odyssey.api.booking.BookingRequestRepository;
import com.odyssey.api.need.Need;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.trip.Trip;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Confirms the exact existing reactions are preserved after moving from
 * {@code @KafkaListener} to {@code @EventListener}: BOOKING_REQUESTED
 * and QUOTE_ACCEPTED both persist a notification and push it to the agent
 * over SSE (real-time notification reaching the React Agent).
 */
class AgentNotificationEventListenerTest {

    private AgentRepository agentRepository;
    private AgentNotificationRepository notificationRepository;
    private BookingRequestRepository bookingRequestRepository;
    private AgentNotificationSseService sseService;
    private AgentNotificationEventListener listener;

    @BeforeEach
    void setUp() {
        agentRepository = mock(AgentRepository.class);
        notificationRepository = mock(AgentNotificationRepository.class);
        bookingRequestRepository = mock(BookingRequestRepository.class);
        sseService = mock(AgentNotificationSseService.class);
        listener = new AgentNotificationEventListener(
            agentRepository,
            notificationRepository,
            bookingRequestRepository,
            sseService
        );
    }

    @Test
    void bookingRequestedCreatesNotificationAndPushesSse() {

        Traveler traveler = mock(Traveler.class);
        when(traveler.getFirstName()).thenReturn("Alice");

        Trip trip = mock(Trip.class);
        when(trip.getTitle()).thenReturn("Prochain voyage à Cuba");
        when(trip.getTraveler()).thenReturn(traveler);

        Need need = mock(Need.class);
        when(need.getType()).thenReturn(com.odyssey.api.need.NeedType.FLIGHT);
        when(need.getTrip()).thenReturn(trip);

        BookingRequest bookingRequest = mock(BookingRequest.class);
        when(bookingRequest.getId()).thenReturn(100L);
        when(bookingRequest.getNeed()).thenReturn(need);
        when(bookingRequestRepository.findById(100L))
            .thenReturn(Optional.of(bookingRequest));

        Agent agent = mock(Agent.class);
        when(agent.getId()).thenReturn(5L);
        when(agentRepository.findByStatus(AgentStatus.AVAILABLE))
            .thenReturn(List.of(agent));

        when(notificationRepository.save(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        listener.onBookingRequested(
            new BookingRequestedEvent(100L, 200L, 300L)
        );

        verify(notificationRepository).save(any(AgentNotification.class));
        verify(sseService).send(eq(5L), any(AgentNotificationResponse.class));
    }

    @Test
    void quoteAcceptedCreatesNotificationAndPushesSse() {

        Traveler traveler = mock(Traveler.class);
        when(traveler.getFirstName()).thenReturn("Alice");

        Trip trip = mock(Trip.class);
        when(trip.getTraveler()).thenReturn(traveler);

        Need need = mock(Need.class);
        when(need.getTrip()).thenReturn(trip);

        BookingRequest bookingRequest = mock(BookingRequest.class);
        when(bookingRequest.getId()).thenReturn(100L);
        when(bookingRequest.getNeed()).thenReturn(need);
        when(bookingRequestRepository.findById(100L))
            .thenReturn(Optional.of(bookingRequest));

        Agent agent = mock(Agent.class);
        when(agent.getId()).thenReturn(5L);
        when(agentRepository.findById(5L)).thenReturn(Optional.of(agent));

        when(notificationRepository.save(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        listener.onQuoteAccepted(
            new QuoteAcceptedEvent(1L, 100L, 300L, 5L)
        );

        verify(notificationRepository).save(any(AgentNotification.class));
        verify(sseService).send(eq(5L), any(AgentNotificationResponse.class));
    }
}
