package com.odyssey.api.intent.recommendation;

import com.odyssey.api.experience.ExperienceCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@Primary
public class AIIntentAnalyzer implements IntentAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(
        AIIntentAnalyzer.class
    );

    private static final String SYSTEM_PROMPT = """
        Odyssey is an experience-first travel platform.
        Your only job is to extract structured intent from a traveler sentence.

        Extract exactly these fields:
        - category: one of DANCE, CULTURE, NATURE, ADVENTURE, FOOD, BEACH,
          ROAD_TRIP, SURF, or null
        - activity: a concrete activity explicitly or reasonably expressed,
          or null
        - destination: a destination supplied by the traveler, or null

        Choose the closest supported category only when reasonably justified.
        Never invent a destination or activity. Do not recommend experiences,
        calculate scores, or perform any other business operation.

        Examples:
        "Je veux apprendre la salsa à Cuba"
        -> category DANCE, activity salsa, destination Cuba

        "Je veux faire du surf au Portugal"
        -> category SURF, activity surf, destination Portugal

        "J'aimerais découvrir Buenos Aires et apprendre le tango"
        -> category DANCE, activity tango, destination Buenos Aires
        """;

    private final ChatClient chatClient;
    private final IntentAnalyzer deterministicAnalyzer;

    @Autowired
    public AIIntentAnalyzer(
        ChatClient.Builder chatClientBuilder,
        IntentAnalyzer deterministicAnalyzer
    ) {
        this.chatClient = chatClientBuilder
            .defaultSystem(SYSTEM_PROMPT)
            .build();
        this.deterministicAnalyzer = deterministicAnalyzer;
    }

    AIIntentAnalyzer(
        ChatClient chatClient,
        IntentAnalyzer deterministicAnalyzer
    ) {
        this.chatClient = chatClient;
        this.deterministicAnalyzer = deterministicAnalyzer;
    }

    @Override
    public AnalyzedIntent analyze(String text) {
        if (text == null || text.isBlank()) {
            return new AnalyzedIntent(null, null, null);
        }

        try {
            AIIntentResult result = chatClient
                .prompt()
                .user(text)
                .call()
                .entity(AIIntentResult.class);

            if (result == null) {
                return fallback(text, "empty AI response");
            }

            ExperienceCategory category = toCategory(result.category());
            if (result.category() != null && category == null) {
                return fallback(text, "unsupported AI category");
            }

            return new AnalyzedIntent(
                category,
                blankToNull(result.activity()),
                blankToNull(result.destination())
            );
        } catch (Exception exception) {
            logger.warn(
                "AI intent analysis failed; using deterministic fallback: {}",
                exception.getClass().getSimpleName()
            );
            return deterministicAnalyzer.analyze(text);
        }
    }

    private ExperienceCategory toCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }

        try {
            return ExperienceCategory.valueOf(
                category.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private AnalyzedIntent fallback(String text, String reason) {
        logger.warn(
            "AI intent analysis returned {}; using deterministic fallback",
            reason
        );
        return deterministicAnalyzer.analyze(text);
    }
}