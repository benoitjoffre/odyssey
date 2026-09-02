package com.odyssey.api.intent.recommendation;

import com.odyssey.api.experience.Experience;
import com.odyssey.api.experience.ExperienceCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecommendationScorerTest {

    private final RecommendationScorer scorer = new RecommendationScorer();

    @Test
    void scoresCategoryActivityAndDestination() {
        Experience experience = new Experience();
        experience.setTitle("Cours de Salsa à La Havane");
        experience.setDescription("Une initiation à la danse cubaine");
        experience.setDestination("Cuba");
        experience.setCategory(ExperienceCategory.DANCE);

        AnalyzedIntent intent = new AnalyzedIntent(
            ExperienceCategory.DANCE,
            "salsa",
            "Cuba"
        );

        assertEquals(100, scorer.score(intent, experience));
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
}