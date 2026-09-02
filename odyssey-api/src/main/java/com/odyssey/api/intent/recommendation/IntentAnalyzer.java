package com.odyssey.api.intent.recommendation;

import com.odyssey.api.experience.ExperienceCategory;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class IntentAnalyzer implements IntentAnalysisService {

    @Override
    public AnalyzedIntent analyze(String text) {
        if (text == null || text.isBlank()) {
            return new AnalyzedIntent(null, null, null);
        }

        String normalized = text.toLowerCase(Locale.ROOT);

        return new AnalyzedIntent(
            detectCategory(text),
            detectActivity(normalized),
            detectDestination(normalized)
        );
    }

    private String detectActivity(String normalized) {
        if (normalized.contains("salsa")) {
            return "salsa";
        }
        if (normalized.contains("bachata")) {
            return "bachata";
        }
        if (normalized.contains("tango")) {
            return "tango";
        }
        if (normalized.contains("surf")) {
            return "surf";
        }
        if (normalized.contains("randonnée")) {
            return "randonnée";
        }
        if (normalized.contains("rafting")) {
            return "rafting";
        }
        if (normalized.contains("cuisine")) {
            return "cuisine";
        }

        return null;
    }

    private String detectDestination(String normalized) {
        if (normalized.contains("buenos aires")) {
            return "Buenos Aires";
        }
        if (
            normalized.contains("la havane") ||
            normalized.contains("havane")
        ) {
            return "La Havane";
        }
        if (normalized.contains("costa rica")) {
            return "Costa Rica";
        }
        if (normalized.contains("lanzarote")) {
            return "Lanzarote";
        }
        if (normalized.contains("cuba")) {
            return "Cuba";
        }
        if (normalized.contains("portugal")) {
            return "Portugal";
        }
        if (normalized.contains("espagne")) {
            return "Espagne";
        }
        if (normalized.contains("argentine")) {
            return "Argentine";
        }

        return null;
    }

    public ExperienceCategory detectCategory(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String normalized = text.toLowerCase(Locale.ROOT);

        if (
            normalized.contains("danse") ||
            normalized.contains("dance") ||
            normalized.contains("salsa") ||
            normalized.contains("bachata") ||
            normalized.contains("tango")
        ) {
            return ExperienceCategory.DANCE;
        }

        if (
            normalized.contains("culture") ||
            normalized.contains("musée") ||
            normalized.contains("patrimoine") ||
            normalized.contains("histoire") ||
            normalized.contains("monument")
        ) {
            return ExperienceCategory.CULTURE;
        }

        if (
            normalized.contains("nature") ||
            normalized.contains("randonnée") ||
            normalized.contains("forêt") ||
            normalized.contains("montagne")
        ) {
            return ExperienceCategory.NATURE;
        }

        if (
            normalized.contains("aventure") ||
            normalized.contains("escalade") ||
            normalized.contains("rafting") ||
            normalized.contains("parapente")
        ) {
            return ExperienceCategory.ADVENTURE;
        }

        if (
            normalized.contains("gastronomie") ||
            normalized.contains("restaurant") ||
            normalized.contains("cuisine") ||
            normalized.contains("dégustation")
        ) {
            return ExperienceCategory.FOOD;
        }

        if (
            normalized.contains("plage") ||
            normalized.contains("baignade") ||
            normalized.contains("bord de mer")
        ) {
            return ExperienceCategory.BEACH;
        }

        if (
            normalized.contains("road trip") ||
            normalized.contains("roadtrip") ||
            normalized.contains("route panoramique")
        ) {
            return ExperienceCategory.ROAD_TRIP;
        }

        if (
            normalized.contains("surf") ||
            normalized.contains("surfing")
        ) {
            return ExperienceCategory.SURF;
        }

        return null;
    }
}
