package com.odyssey.api.intent;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.odyssey.api.intent.recommendation.IntentAnalysisService;
import com.odyssey.api.intent.recommendation.IntentAnalyzer;
import com.odyssey.api.intent.recommendation.RecommendationScorer;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerRepository;

@ExtendWith(MockitoExtension.class)
class IntentServiceTest {

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
        when(travelerRepository.findByAuth0Subject("auth0|traveler-a"))
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
            "auth0|traveler-a"
        );

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(intentRepository).save(captor.capture());
        assertEquals(traveler, captor.getValue().getTraveler());
        assertEquals(1L, response.travelerId());
        verify(travelerRepository).findByAuth0Subject("auth0|traveler-a");
    }
}
