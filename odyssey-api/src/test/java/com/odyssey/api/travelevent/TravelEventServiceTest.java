package com.odyssey.api.travelevent;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.experience.ExperienceRepository;
import com.odyssey.api.trip.TripRepository;

@ExtendWith(MockitoExtension.class)
class TravelEventServiceTest {

    @Mock
    private TravelEventRepository travelEventRepository;

    @Mock
    private ExperienceRepository experienceRepository;

    @Mock
    private TripRepository tripRepository;

    private TravelEventService travelEventService;

    @BeforeEach
    void setUp() {
        travelEventService = new TravelEventService(
            travelEventRepository,
            experienceRepository,
            tripRepository
        );
    }

    @Test
    void deleteUnlinksTripsBeforeDeletingEvent() {
        TravelEvent event = new TravelEvent();
        when(travelEventRepository.findById(10L)).thenReturn(Optional.of(event));

        travelEventService.delete(10L);

        verify(tripRepository).clearTravelEventByTravelEventId(10L);
        verify(travelEventRepository).delete(event);
    }

    @Test
    void deleteThrowsWhenEventDoesNotExist() {
        when(travelEventRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> travelEventService.delete(10L));
    }
}
