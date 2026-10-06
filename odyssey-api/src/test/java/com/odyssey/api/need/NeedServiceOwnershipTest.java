package com.odyssey.api.need;

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
import org.springframework.test.util.ReflectionTestUtils;

import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.need.accommodation.AccommodationCriteriaRepository;
import com.odyssey.api.need.flight.FlightCriteriaRepository;
import com.odyssey.api.need.transfer.TransferCriteriaRepository;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerRepository;
import com.odyssey.api.trip.Trip;
import com.odyssey.api.trip.TripRepository;

@ExtendWith(MockitoExtension.class)
class NeedServiceOwnershipTest {

    @Mock
    private NeedRepository needRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TravelerRepository travelerRepository;

    @Mock
    private FlightCriteriaRepository flightCriteriaRepository;

    @Mock
    private AccommodationCriteriaRepository accommodationCriteriaRepository;

    @Mock
    private TransferCriteriaRepository transferCriteriaRepository;

    private NeedService needService;

    @BeforeEach
    void setUp() {
        needService = new NeedService(
            needRepository,
            tripRepository,
            travelerRepository,
            flightCriteriaRepository,
            accommodationCriteriaRepository,
            transferCriteriaRepository
        );
    }

    @Test
    void getNeedRejectsNeedOwnedByAnotherTraveler() {
        Traveler travelerA = new Traveler("Alice", "A", "alice@example.com");
        travelerA.setId(1L);
        Traveler travelerB = new Traveler("Bob", "B", "bob@example.com");
        travelerB.setId(2L);

        Trip tripB = new Trip();
        tripB.setTraveler(travelerB);

        Need need = new Need();
        ReflectionTestUtils.setField(need, "id", 50L);
        need.setTrip(tripB);

        when(travelerRepository.findByAuth0Subject("auth0|traveler-a"))
            .thenReturn(Optional.of(travelerA));
        when(needRepository.findById(50L)).thenReturn(Optional.of(need));

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> needService.getNeed(50L, "auth0|traveler-a")
        );

        assertEquals("Need not found", exception.getMessage());
        verify(needRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
