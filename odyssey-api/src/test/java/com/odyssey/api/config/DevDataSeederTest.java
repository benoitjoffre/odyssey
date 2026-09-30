package com.odyssey.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.odyssey.api.destination.Destination;
import com.odyssey.api.destination.DestinationRepository;
import com.odyssey.api.experience.Experience;
import com.odyssey.api.experience.ExperienceCategory;
import com.odyssey.api.experience.ExperienceRepository;
import com.odyssey.api.travelevent.TravelEvent;
import com.odyssey.api.travelevent.TravelEventRepository;

@ExtendWith(MockitoExtension.class)
class DevDataSeederTest {

    @Mock
    private DestinationRepository destinationRepository;

    @Mock
    private ExperienceRepository experienceRepository;

    @Mock
    private TravelEventRepository travelEventRepository;

    private final Map<String, Destination> destinations = new HashMap<>();
    private final Map<String, Experience> experiences = new HashMap<>();
    private final Map<String, TravelEvent> events = new HashMap<>();

    private long destinationIdSequence;
    private long experienceIdSequence;
    private long eventIdSequence;

    private DevDataSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new DevDataSeeder(
            destinationRepository,
            experienceRepository,
            travelEventRepository
        );

        when(destinationRepository.findByCityIgnoreCaseAndCountryCodeIgnoreCase(any(), any()))
            .thenAnswer(invocation -> {
                String city = ((String) invocation.getArgument(0)).toLowerCase(Locale.ROOT);
                String countryCode = ((String) invocation.getArgument(1)).toLowerCase(Locale.ROOT);
                return Optional.ofNullable(destinations.get(city + "|" + countryCode));
            });
        when(destinationRepository.save(any(Destination.class)))
            .thenAnswer(invocation -> {
                Destination destination = invocation.getArgument(0);
                if (destination.getId() == null) {
                    ReflectionTestUtils.setField(destination, "id", ++destinationIdSequence);
                }
                destinations.put(
                    destination.getCity().toLowerCase(Locale.ROOT) + "|" + destination.getCountryCode().toLowerCase(Locale.ROOT),
                    destination
                );
                return destination;
            });

        when(experienceRepository.findByTitleIgnoreCase(any()))
            .thenAnswer(invocation -> Optional.ofNullable(
                experiences.get(((String) invocation.getArgument(0)).toLowerCase(Locale.ROOT))
            ));
        when(experienceRepository.save(any(Experience.class)))
            .thenAnswer(invocation -> {
                Experience experience = invocation.getArgument(0);
                if (experience.getId() == null) {
                    ReflectionTestUtils.setField(experience, "id", ++experienceIdSequence);
                }
                experiences.put(experience.getTitle().toLowerCase(Locale.ROOT), experience);
                return experience;
            });

        when(travelEventRepository.findByExperienceIdAndNameIgnoreCase(any(), any()))
            .thenAnswer(invocation -> {
                Long experienceId = invocation.getArgument(0);
                String name = ((String) invocation.getArgument(1)).toLowerCase(Locale.ROOT);
                return Optional.ofNullable(events.get(experienceId + "|" + name));
            });
        when(travelEventRepository.save(any(TravelEvent.class)))
            .thenAnswer(invocation -> {
                TravelEvent event = invocation.getArgument(0);
                if (event.getId() == null) {
                    ReflectionTestUtils.setField(event, "id", ++eventIdSequence);
                }
                events.put(
                    event.getExperience().getId() + "|" + event.getName().toLowerCase(Locale.ROOT),
                    event
                );
                return event;
            });
    }

    @Test
    void seederIsIdempotentAcrossTwoRuns() {
        seeder.run();
        assertEquals(6, destinations.size());
        assertEquals(6, experiences.size());
        assertEquals(12, events.size());

        seeder.run();
        assertEquals(6, destinations.size());
        assertEquals(6, experiences.size());
        assertEquals(12, events.size());
    }

    @Test
    void seededExperiencesUseDestinationRelationAndDoNotUseLegacyDestination() {
        seeder.run();

        assertFalse(experiences.isEmpty());
        experiences.values().forEach(experience -> {
            assertNotNull(experience.getDestinationEntity());
            assertNull(experience.getLegacyDestination());
            assertNotNull(experience.getDestinationEntity().getCity());
            assertNotNull(experience.getDestinationEntity().getCountryCode());
        });
    }

    @Test
    void seededTravelEventsAreLinkedToExperiences() {
        seeder.run();

        assertFalse(events.isEmpty());
        events.values().forEach(event -> {
            assertNotNull(event.getExperience());
            assertNotNull(event.getExperience().getDestinationEntity());
            assertNotNull(event.getStartDate());
            assertNotNull(event.getEndDate());
            assertFalse(event.getStartDate().isAfter(event.getEndDate()));
            assertNotNull(event.getLocation());
        });
    }
}
