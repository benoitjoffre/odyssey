package com.odyssey.api.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.odyssey.api.agent.AgentRepository;
import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.need.Need;
import com.odyssey.api.need.NeedRepository;
import com.odyssey.api.need.accommodation.AccommodationCriteriaRepository;
import com.odyssey.api.need.flight.FlightCriteriaRepository;
import com.odyssey.api.outbox.OutboxEventRepository;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerRepository;
import com.odyssey.api.trip.Trip;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class BookingRequestServiceOwnershipTest {

    @Mock
    private BookingRequestRepository bookingRequestRepository;

    @Mock
    private NeedRepository needRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private TravelerRepository travelerRepository;

    @Mock
    private FlightCriteriaRepository flightCriteriaRepository;

    @Mock
    private AccommodationCriteriaRepository accommodationCriteriaRepository;

    private BookingRequestService bookingRequestService;

    @BeforeEach
    void setUp() {
        bookingRequestService = new BookingRequestService(
            bookingRequestRepository,
            needRepository,
            outboxEventRepository,
            new ObjectMapper(),
            agentRepository,
            travelerRepository,
            flightCriteriaRepository,
            accommodationCriteriaRepository
        );
    }

    @Test
    void createBookingRequestRejectsNeedOwnedByAnotherTraveler() {
        Traveler travelerA = new Traveler("Alice", "A", "alice@example.com");
        travelerA.setId(1L);
        Traveler travelerB = new Traveler("Bob", "B", "bob@example.com");
        travelerB.setId(2L);

        Trip tripB = new Trip();
        tripB.setTraveler(travelerB);

        Need need = new Need();
        need.setTrip(tripB);

        when(travelerRepository.findByAuth0Subject("auth0|traveler-a"))
            .thenReturn(Optional.of(travelerA));
        when(needRepository.findById(10L)).thenReturn(Optional.of(need));

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> bookingRequestService.createBookingRequest(
                new CreateBookingRequest(10L, "notes"),
                "auth0|traveler-a"
            )
        );

        assertEquals("Need not found", exception.getMessage());
        verify(bookingRequestRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(outboxEventRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
