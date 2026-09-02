package com.odyssey.api.intent.recommendation;

import java.text.Normalizer;
import java.util.Locale;

public class TextMatcher {

    public boolean containsTerm(String text, String term) {
        if (
            text == null || text.isBlank() ||
            term == null || term.isBlank()
        ) {
            return false;
        }

        String normalizedText = normalize(text);
        String normalizedTerm = normalize(term);

        if (normalizedText.isBlank() || normalizedTerm.isBlank()) {
            return false;
        }

        return (" " + normalizedText + " ")
            .contains(" " + normalizedTerm + " ");
    }

    private String normalize(String value) {
        return Normalizer
            .normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{L}\\p{N}]+", " ")
            .trim()
            .replaceAll("\\s+", " ");
    }
}