package com.odyssey.api.intent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.odyssey.api.experience.ExperienceCategory;
import com.odyssey.api.experience.ExperienceRepository;
import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.intent.recommendation.IntentAnalysisService;
import com.odyssey.api.intent.recommendation.IntentAnalyzer;
import com.odyssey.api.intent.recommendation.RecommendationScorer;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerRepository;

@ExtendWith(MockitoExtension.class)
class IntentServiceTest {

    private static final String AUTH0_SUBJECT = "auth0|traveler-a";

    @Mock private IntentRepository intentRepository;
    @Mock private TravelerRepository travelerRepository;
    @Mock private ExperienceRepository experienceRepository;
    @Mock private IntentAnalyzer intentAnalyzer;
    @Mock private IntentAnalysisService intentAnalysisService;
    @Mock private RecommendationScorer recommendationScorer;

    private IntentService intentService;

    @BeforeEach
    void setUp() {
        intentService = new IntentService(
            intentRepository,
            travelerRepository,
            experienceRepository,
            intentAnalyzer,
            intentAnalysisService,
            recommendationScorer
        );
    }

    @Test
    void createIntentUsesAuthenticatedTraveler() {
        Traveler traveler = new Traveler(
            "Alice",
            "A",
            "alice@example.com"
        );
        ReflectionTestUtils.setField(traveler, "id", 1L);
        when(travelerRepository.findByAuth0Subject(AUTH0_SUBJECT))
            .thenReturn(Optional.of(traveler));
        when(intentRepository.save(any(Intent.class))).thenAnswer(invocation -> {
            Intent intent = invocation.getArgument(0);
            ReflectionTestUtils.setField(intent, "id", 100L);
            return intent;
        });
        CreateIntentRequest request = new CreateIntentRequest(
            "Surf trip",
            "I want to surf in Portugal",
            ExperienceCategory.SURF
        );

        IntentResponse response = intentService.createIntent(
            request,
            AUTH0_SUBJECT
        );

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(intentRepository).save(captor.capture());
        assertEquals(traveler, captor.getValue().getTraveler());
        assertEquals(1L, response.travelerId());
        verify(travelerRepository).findByAuth0Subject(AUTH0_SUBJECT);
    }

    @Test
    void getIntentsReturnsOnlyCurrentTravelerIntents() {
        Traveler traveler = new Traveler("Alice", "A", "alice@example.com");
        ReflectionTestUtils.setField(traveler, "id", 9L);
        when(travelerRepository.findByAuth0Subject(AUTH0_SUBJECT))
            .thenReturn(Optional.of(traveler));

        Intent intent = new Intent();
        ReflectionTestUtils.setField(intent, "id", 100L);
        intent.setTitle("Intent 1");
        intent.setDescription("Desc");
        intent.setStatus(IntentStatus.DRAFT);
        intent.setCategory(ExperienceCategory.CULTURE);
        intent.setTraveler(traveler);

        when(intentRepository.findByTravelerIdOrderByIdDesc(9L))
            .thenReturn(java.util.List.of(intent));

        var intents = intentService.getIntents(AUTH0_SUBJECT);

        assertEquals(1, intents.size());
        assertEquals(100L, intents.get(0).id());
    }

    @Test
    void getIntentRejectsWhenIntentNotOwnedByTraveler() {
        Traveler traveler = new Traveler("Alice", "A", "alice@example.com");
        ReflectionTestUtils.setField(traveler, "id", 9L);
        when(travelerRepository.findByAuth0Subject(AUTH0_SUBJECT))
            .thenReturn(Optional.of(traveler));
        when(intentRepository.findByIdAndTravelerId(50L, 9L))
            .thenReturn(Optional.empty());

        assertThrows(
            ResourceNotFoundException.class,
            () -> intentService.getIntent(50L, AUTH0_SUBJECT)
        );
    }
}
