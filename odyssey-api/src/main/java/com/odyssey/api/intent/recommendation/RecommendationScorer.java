package com.odyssey.api.intent.recommendation;

import com.odyssey.api.experience.Experience;
import org.springframework.stereotype.Service;

@Service
public class RecommendationScorer {

    private final TextMatcher textMatcher = new TextMatcher();

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
            (textMatcher.containsTerm(experience.getTitle(), intent.activity()) ||
                textMatcher.containsTerm(
                    experience.getDescription(),
                    intent.activity()
                ))
        ) {
            score += 30;
        }

        if (
            intent.destination() != null &&
            (textMatcher.containsTerm(
                experience.getDestination(),
                intent.destination()
            ) || textMatcher.containsTerm(
                experience.getTitle(),
                intent.destination()
            ) || textMatcher.containsTerm(
                experience.getDescription(),
                intent.destination()
            ))
        ) {
            score += 20;
        }

        return score;
    }
}