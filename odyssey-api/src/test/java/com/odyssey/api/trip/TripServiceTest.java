package com.odyssey.api.trip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.odyssey.api.booking.BookingRequestRepository;
import com.odyssey.api.booking.confirmation.BookingRepository;
import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.need.NeedRepository;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerRepository;
import com.odyssey.api.travelevent.TravelEventRepository;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    private static final String TRAVELER_A_SUBJECT = "auth0|traveler-a";

    @Mock private TripRepository tripRepository;
    @Mock private TravelerRepository travelerRepository;
    @Mock private NeedRepository needRepository;
    @Mock private BookingRequestRepository bookingRequestRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private TravelEventRepository travelEventRepository;

    private TripService tripService;
    private Traveler travelerA;
    private Trip tripA;

    @BeforeEach
    void setUp() {
        tripService = new TripService(
            tripRepository,
            travelerRepository,
            needRepository,
            bookingRequestRepository,
            bookingRepository,
            travelEventRepository
        );
        travelerA = traveler(1L, "Alice", "A");
        tripA = trip(10L, travelerA);
    }

    @Test
    void travelerCanGetOwnedTrip() {
        mockTravelerA();
        when(tripRepository.findByIdAndTravelerId(10L, 1L))
            .thenReturn(Optional.of(tripA));

        TripResponse response = tripService.getTrip(10L, TRAVELER_A_SUBJECT);

        assertEquals(10L, response.id());
        assertEquals(1L, response.travelerId());
    }

    @Test
    void travelerCannotGetAnotherTravelersTrip() {
        mockTravelerA();
        when(tripRepository.findByIdAndTravelerId(20L, 1L))
            .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> tripService.getTrip(20L, TRAVELER_A_SUBJECT)
        );

        assertEquals("Trip not found", exception.getMessage());
    }

    @Test
    void travelerCanDeleteOwnedTrip() {
        mockTravelerA();
        when(tripRepository.findByIdAndTravelerId(10L, 1L))
            .thenReturn(Optional.of(tripA));

        tripService.deleteTrip(10L, TRAVELER_A_SUBJECT);

        verify(tripRepository).delete(tripA);
    }

    @Test
    void travelerCannotDeleteAnotherTravelersTrip() {
        mockTravelerA();
        when(tripRepository.findByIdAndTravelerId(20L, 1L))
            .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> tripService.deleteTrip(20L, TRAVELER_A_SUBJECT)
        );

        assertEquals("Trip not found", exception.getMessage());
        verify(tripRepository, never()).delete(any());
    }

    @Test
    void travelerCanGetOwnedTripDetail() {
        mockTravelerA();
        when(tripRepository.findByIdAndTravelerId(10L, 1L))
            .thenReturn(Optional.of(tripA));
        when(needRepository.findByTripId(10L)).thenReturn(List.of());

        TripDetailResponse response = tripService.getTripDetail(
            10L,
            TRAVELER_A_SUBJECT
        );

        assertEquals(10L, response.id());
        assertEquals(1L, response.travelerId());
    }

    @Test
    void travelerCannotGetAnotherTravelersTripDetail() {
        mockTravelerA();
        when(tripRepository.findByIdAndTravelerId(20L, 1L))
            .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> tripService.getTripDetail(20L, TRAVELER_A_SUBJECT)
        );

        assertEquals("Trip not found", exception.getMessage());
        verify(needRepository, never()).findByTripId(any());
    }

    @Test
    void createTripUsesAuthenticatedTraveler() {
        mockTravelerA();
        CreateTripRequest request = new CreateTripRequest(
            "Trip A",
            LocalDate.now().plusDays(10),
            LocalDate.now().plusDays(15),
            null
        );
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
            Trip trip = invocation.getArgument(0);
            ReflectionTestUtils.setField(trip, "id", 10L);
            return trip;
        });

        TripResponse response = tripService.createTrip(
            request,
            TRAVELER_A_SUBJECT
        );

        assertEquals(1L, response.travelerId());
        verify(travelerRepository).findByAuth0Subject(TRAVELER_A_SUBJECT);
    }

    private void mockTravelerA() {
        when(travelerRepository.findByAuth0Subject(TRAVELER_A_SUBJECT))
            .thenReturn(Optional.of(travelerA));
    }

    private Traveler traveler(Long id, String firstName, String lastName) {
        Traveler traveler = new Traveler(
            firstName,
            lastName,
            firstName + "@example.com"
        );
        ReflectionTestUtils.setField(traveler, "id", id);
        return traveler;
    }

    private Trip trip(Long id, Traveler traveler) {
        Trip trip = new Trip();
        ReflectionTestUtils.setField(trip, "id", id);
        trip.setTitle("Trip " + id);
        trip.setStartDate(LocalDate.now().plusDays(10));
        trip.setEndDate(LocalDate.now().plusDays(15));
        trip.setStatus(TripStatus.DRAFT);
        trip.setTraveler(traveler);
        return trip;
    }
}