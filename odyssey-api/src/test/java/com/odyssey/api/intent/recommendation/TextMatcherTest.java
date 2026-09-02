package com.odyssey.api.intent.recommendation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextMatcherTest {

    private final TextMatcher matcher = new TextMatcher();

    @Test
    void matchesCompleteNormalizedTerms() {
        assertTrue(matcher.containsTerm("Voyage à Cuba", "Cuba"));
        assertTrue(matcher.containsTerm("Immersion Salsa", "salsa"));
        assertTrue(matcher.containsTerm("Découverte de la Havane", "La Havane"));
        assertTrue(matcher.containsTerm("LA HAVANE", "la havane"));
        assertTrue(matcher.containsTerm(
            "Dégustation de cuisine locale",
            "degustation"
        ));
    }

    @Test
    void rejectsTermsContainedInsideOtherWords() {
        assertFalse(matcher.containsTerm("salsa cubaine", "Cuba"));
        assertFalse(matcher.containsTerm("surfeur", "surf"));
    }

    @Test
    void rejectsNullOrBlankValues() {
        assertFalse(matcher.containsTerm(null, "surf"));
        assertFalse(matcher.containsTerm("surf", null));
        assertFalse(matcher.containsTerm("", "surf"));
        assertFalse(matcher.containsTerm("surf", "  "));
    }
}