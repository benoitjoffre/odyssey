package com.odyssey.api.intent;

import com.odyssey.api.experience.Experience;
import com.odyssey.api.experience.ExperienceCategory;
import com.odyssey.api.experience.ExperienceRepository;
import com.odyssey.api.intent.recommendation.AnalyzedIntent;
import com.odyssey.api.intent.recommendation.IntentAnalysisService;
import com.odyssey.api.intent.recommendation.IntentAnalyzer;
import com.odyssey.api.intent.recommendation.RecommendationScorer;
import com.odyssey.api.intent.recommendation.ScoredExperienceResponse;
import com.odyssey.api.traveler.TravelerRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IntentServiceRecommendationTest {

    @Test
    void scoresFiltersAndSortsUsingPersistedCategoryAsFallback() {
        IntentRepository intentRepository = mock(IntentRepository.class);
        TravelerRepository travelerRepository = mock(TravelerRepository.class);
        ExperienceRepository experienceRepository = mock(ExperienceRepository.class);
        IntentAnalyzer intentAnalyzer = mock(IntentAnalyzer.class);
        IntentAnalysisService intentAnalysisService = mock(
            IntentAnalysisService.class
        );

        Intent intent = mock(Intent.class);
        when(intent.getDescription()).thenReturn("Salsa à Cuba");
        when(intent.getCategory()).thenReturn(ExperienceCategory.DANCE);
        when(intentRepository.findById(1L)).thenReturn(Optional.of(intent));
        when(intentAnalysisService.analyze("Salsa à Cuba"))
            .thenReturn(new AnalyzedIntent(null, "salsa", "Cuba"));

        Experience fullMatch = experience(
            "Cours de salsa",
            "Danse cubaine",
            "Cuba",
            ExperienceCategory.DANCE
        );
        Experience categoryMatch = experience(
            "Spectacle local",
            null,
            "Espagne",
            ExperienceCategory.DANCE
        );
        Experience noMatch = experience(
            "Visite de musée",
            null,
            "Portugal",
            ExperienceCategory.CULTURE
        );
        when(experienceRepository.findAll())
            .thenReturn(List.of(categoryMatch, noMatch, fullMatch));

        IntentService service = new IntentService(
            intentRepository,
            travelerRepository,
            experienceRepository,
            intentAnalyzer,
            intentAnalysisService,
            new RecommendationScorer()
        );

        List<ScoredExperienceResponse> recommendations =
            service.getRecommendations(1L);

        assertEquals(2, recommendations.size());
        assertEquals(100, recommendations.get(0).score());
        assertEquals("Cours de salsa", recommendations.get(0).title());
        assertEquals(50, recommendations.get(1).score());
    }

    private Experience experience(
        String title,
        String description,
        String destination,
        ExperienceCategory category
    ) {
        Experience experience = new Experience();
        experience.setTitle(title);
        experience.setDescription(description);
        experience.setDestination(destination);
        experience.setCategory(category);
        return experience;
    }
}