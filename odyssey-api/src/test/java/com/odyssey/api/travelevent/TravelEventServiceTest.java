package com.odyssey.api.travelevent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.odyssey.api.experience.Experience;
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

    @Test
    void createPersistsAndReturnsImageUrl() {
        Experience experience = mock(Experience.class);
        when(experience.getId()).thenReturn(2L);
        when(experienceRepository.findById(2L)).thenReturn(Optional.of(experience));
        when(travelEventRepository.save(any(TravelEvent.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        TravelEventResponse response = travelEventService.create(new CreateTravelEventRequest(
            "Festival",
            "Paris",
            LocalDate.parse("2026-10-10"),
            LocalDate.parse("2026-10-12"),
            null,
            "https://res.cloudinary.com/example/image/upload/festival.jpg",
            2L
        ));

        assertEquals("https://res.cloudinary.com/example/image/upload/festival.jpg", response.imageUrl());
    }

    @Test
    void updateChangesEventFieldsAndImage() {
        Experience experience = mock(Experience.class);
        when(experience.getId()).thenReturn(2L);
        when(experienceRepository.findById(2L)).thenReturn(Optional.of(experience));

        TravelEvent event = new TravelEvent();
        when(travelEventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(travelEventRepository.save(any(TravelEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TravelEventResponse response = travelEventService.update(10L, new CreateTravelEventRequest(
            "Festival mis à jour",
            "Lyon",
            LocalDate.parse("2026-10-11"),
            LocalDate.parse("2026-10-13"),
            "Nouvelle description",
            "https://res.cloudinary.com/example/image/upload/updated.jpg",
            2L
        ));

        assertEquals("Festival mis à jour", event.getName());
        assertEquals("Lyon", event.getLocation());
        assertEquals("Nouvelle description", event.getDescription());
        assertEquals("https://res.cloudinary.com/example/image/upload/updated.jpg", response.imageUrl());
    }

    @Test
    void updateRejectsInvalidDateRange() {
        CreateTravelEventRequest request = new CreateTravelEventRequest(
            "Festival",
            "Paris",
            LocalDate.parse("2026-10-13"),
            LocalDate.parse("2026-10-11"),
            null,
            null,
            2L
        );

        assertThrows(IllegalArgumentException.class, () -> travelEventService.update(10L, request));
        verifyNoInteractions(travelEventRepository, experienceRepository);
    }
}
