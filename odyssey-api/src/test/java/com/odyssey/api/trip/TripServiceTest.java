package com.odyssey.api.trip;

import com.odyssey.api.booking.BookingRequestRepository;
import com.odyssey.api.booking.confirmation.BookingRepository;
import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.need.NeedRepository;
import com.odyssey.api.traveler.TravelerRepository;
import com.odyssey.api.travelevent.TravelEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TravelerRepository travelerRepository;

    @Mock
    private NeedRepository needRepository;

    @Mock
    private BookingRequestRepository bookingRequestRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private TravelEventRepository travelEventRepository;

    private TripService tripService;

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
    }

    @Test
    void deleteTripDeletesExistingTrip() {
        Trip trip = new Trip();
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        tripService.deleteTrip(1L);

        verify(tripRepository).delete(trip);
    }

    @Test
    void deleteTripThrowsWhenTripDoesNotExist() {
        when(tripRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
            ResourceNotFoundException.class,
            () -> tripService.deleteTrip(1L)
        );
        verify(tripRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }
}