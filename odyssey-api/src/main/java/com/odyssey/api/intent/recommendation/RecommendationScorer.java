package com.odyssey.api.intent.recommendation;

import com.odyssey.api.destination.Destination;
import com.odyssey.api.experience.Experience;
import org.springframework.stereotype.Service;

import static org.springframework.util.StringUtils.hasText;

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
                destinationText(experience),
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

    private String destinationText(Experience experience) {
        Destination destination = experience.getDestinationEntity();
        if (destination != null) {
            return String.join(
                " ",
                nullToEmpty(destination.getCity()),
                nullToEmpty(destination.getCountry()),
                nullToEmpty(destination.getCountryCode())
            );
        }

        if (hasText(experience.getLegacyDestination())) {
            return experience.getLegacyDestination();
        }

        return null;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}