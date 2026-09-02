package com.odyssey.api.intent.recommendation;

import com.odyssey.api.experience.Experience;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class RecommendationScorer {

    public int score(AnalyzedIntent intent, Experience experience) {
        if (intent == null || experience == null) {
            return 0;
        }

        int score = 0;

        if (
            intent.category() != null &&
            intent.category() == experience.getCategory()
        ) {
            score += 50;
        }

        if (
            intent.activity() != null &&
            (contains(experience.getTitle(), intent.activity()) ||
                contains(experience.getDescription(), intent.activity()))
        ) {
            score += 30;
        }

        if (
            intent.destination() != null &&
            (contains(experience.getDestination(), intent.destination()) ||
                contains(experience.getTitle(), intent.destination()) ||
                contains(experience.getDescription(), intent.destination()))
        ) {
            score += 20;
        }

        return score;
    }

    private boolean contains(String value, String expected) {
        return value != null && value
            .toLowerCase(Locale.ROOT)
            .contains(expected.toLowerCase(Locale.ROOT));
    }
}