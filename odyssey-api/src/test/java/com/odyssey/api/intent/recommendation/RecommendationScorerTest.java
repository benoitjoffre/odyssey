package com.odyssey.api.intent.recommendation;

import com.odyssey.api.experience.Experience;
import com.odyssey.api.experience.ExperienceCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecommendationScorerTest {

    private final RecommendationScorer scorer = new RecommendationScorer();
    private final AnalyzedIntent danceIntent = new AnalyzedIntent(
        ExperienceCategory.DANCE,
        "salsa",
        "Cuba"
    );

    @Test
    void scoresCategoryActivityAndDestination() {
        Experience experience = experience(
            ExperienceCategory.DANCE,
            "Immersion Salsa à Cuba",
            null,
            "Cuba"
        );

        assertEquals(100, scorer.score(danceIntent, experience));
    }

    @Test
    void doesNotMatchDestinationInsideAnotherWord() {
        Experience experience = experience(
            ExperienceCategory.DANCE,
            "Immersion Salsa",
            "Découvrez la salsa cubaine au coeur de La Havane",
            "La Havane"
        );

        assertEquals(80, scorer.score(danceIntent, experience));
    }

    @Test
    void scoresOnlyCategoryWhenTextDoesNotMatch() {
        Experience experience = experience(
            ExperienceCategory.DANCE,
            "Stage de danse à Barcelone",
            "Une expérience autour de la danse",
            "Espagne"
        );

        assertEquals(50, scorer.score(danceIntent, experience));
    }

    @Test
    void returnsZeroWhenNothingMatches() {
        Experience experience = experience(
            ExperienceCategory.SURF,
            "Surf au Portugal",
            null,
            "Portugal"
        );

        assertEquals(0, scorer.score(danceIntent, experience));
    }

    @Test
    void handlesNullFieldsSafely() {
        Experience experience = new Experience();

        assertEquals(
            0,
            scorer.score(new AnalyzedIntent(null, null, null), experience)
        );
        assertEquals(0, scorer.score(null, experience));
        assertEquals(0, scorer.score(new AnalyzedIntent(null, null, null), null));
    }

    private Experience experience(
        ExperienceCategory category,
        String title,
        String description,
        String destination
    ) {
        Experience experience = new Experience();
        experience.setCategory(category);
        experience.setTitle(title);
        experience.setDescription(description);
        experience.setDestination(destination);
        return experience;
    }
}