package com.odyssey.api.experience;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

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
