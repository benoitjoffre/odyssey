package com.odyssey.api.experience;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.odyssey.api.destination.Destination;
import com.odyssey.api.destination.DestinationRepository;
import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.travelevent.TravelEvent;
import com.odyssey.api.travelevent.TravelEventRepository;
import com.odyssey.api.trip.TripRepository;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ExperienceServiceTest {

    @Mock
    private ExperienceRepository experienceRepository;

    @Mock
    private DestinationRepository destinationRepository;

    @Mock
    private TravelEventRepository travelEventRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private ExperienceService experienceService;

    @BeforeEach
    void setUp() {
        experienceService = new ExperienceService(
            destinationRepository,
            experienceRepository,
            travelEventRepository,
            tripRepository,
            redisTemplate,
            objectMapper
        );
    }

    @Test
    void deleteExperienceUnlinksTripsAndDeletesRelatedEvents() {
        Experience experience = new Experience();
        when(experienceRepository.findById(42L)).thenReturn(Optional.of(experience));

        TravelEvent firstEvent = new TravelEvent();
        ReflectionTestUtils.setField(firstEvent, "id", 1L);
        TravelEvent secondEvent = new TravelEvent();
        ReflectionTestUtils.setField(secondEvent, "id", 2L);
        List<TravelEvent> events = List.of(firstEvent, secondEvent);
        when(travelEventRepository.findByExperienceId(42L)).thenReturn(events);

        experienceService.deleteExperience(42L);

        verify(tripRepository).clearTravelEventByTravelEventIds(eq(List.of(1L, 2L)));
        verify(travelEventRepository).deleteAll(events);
        verify(experienceRepository).delete(experience);
        verify(redisTemplate).delete("experience:42");
    }

    @Test
    void deleteExperienceWithoutEventsDeletesExperienceOnly() {
        Experience experience = new Experience();
        when(experienceRepository.findById(42L)).thenReturn(Optional.of(experience));
        when(travelEventRepository.findByExperienceId(42L)).thenReturn(List.of());

        experienceService.deleteExperience(42L);

        verify(tripRepository, never()).clearTravelEventByTravelEventIds(anyList());
        verify(travelEventRepository, never()).deleteAll(anyList());
        verify(experienceRepository).delete(experience);
        verify(redisTemplate).delete("experience:42");
    }

    @Test
    void deleteExperienceThrowsWhenNotFound() {
        when(experienceRepository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> experienceService.deleteExperience(42L));
    }

    @Test
    void createExperienceWithValidDestinationId() {
        Destination destination = new Destination();
        ReflectionTestUtils.setField(destination, "id", 4L);
        destination.setCity("Barcelona");
        destination.setCountry("Espagne");
        destination.setCountryCode("ES");

        when(destinationRepository.findById(4L)).thenReturn(Optional.of(destination));
        when(experienceRepository.save(org.mockito.ArgumentMatchers.any(Experience.class)))
            .thenAnswer(invocation -> {
                Experience saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 10L);
                return saved;
            });

        CreateExperienceRequest request = new CreateExperienceRequest(
            "Barcelone entre architecture et tapas",
            "Experience culture et gastronomie",
            4L,
            ExperienceCategory.CULTURE,
            4
        );

        ExperienceResponse response = experienceService.createExperience(request);

        assertNotNull(response.destination());
        assertEquals(4L, response.destination().id());
        assertEquals("Barcelona", response.destination().city());
        assertEquals("ES", response.destination().countryCode());
    }

    @Test
    void createExperienceThrowsWhenDestinationIdDoesNotExist() {
        when(destinationRepository.findById(99L)).thenReturn(Optional.empty());

        CreateExperienceRequest request = new CreateExperienceRequest(
            "Trip",
            "Desc",
            99L,
            ExperienceCategory.CULTURE,
            3
        );

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> experienceService.createExperience(request)
        );

        assertEquals("Destination not found", exception.getMessage());
    }

    @Test
    void mapsLegacyDestinationWhenRelationIsMissing() {
        Experience experience = new Experience();
        ReflectionTestUtils.setField(experience, "id", 15L);
        experience.setTitle("Legacy experience");
        experience.setDescription("Legacy description");
        experience.setLegacyDestination("Cuba");
        experience.setCategory(ExperienceCategory.CULTURE);
        experience.setDurationDays(5);

        when(experienceRepository.findById(15L)).thenReturn(Optional.of(experience));

        ExperienceResponse response = experienceService.getExperience(15L);

        assertNotNull(response.destination());
        assertNull(response.destination().id());
        assertEquals("Cuba", response.destination().city());
    }

    @Test
    void deleteExperienceContinuesWhenRedisDeleteFails() {
        Experience experience = new Experience();
        when(experienceRepository.findById(42L)).thenReturn(Optional.of(experience));
        when(travelEventRepository.findByExperienceId(42L)).thenReturn(List.of());
        doThrow(new RuntimeException("redis down"))
            .when(redisTemplate)
            .delete("experience:42");

        experienceService.deleteExperience(42L);

        verify(experienceRepository).delete(experience);
        verify(redisTemplate).delete("experience:42");
    }
}
