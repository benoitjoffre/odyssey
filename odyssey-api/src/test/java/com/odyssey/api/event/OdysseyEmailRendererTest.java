package com.odyssey.api.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OdysseyEmailRendererTest {

    private final OdysseyEmailRenderer renderer = new OdysseyEmailRenderer(
        "http://localhost:5173"
    );

    @Test
    void travelerOnboardingCompletedRendersWelcomeEmail() {

        EmailMessage message = renderer.travelerOnboardingCompleted(
            "alice@example.com",
            "fr",
            "Alice"
        );

        assertEquals("alice@example.com", message.to());
        assertEquals("Bienvenue chez Odyssey", message.subject());
        assertTrue(message.htmlBody().contains("Bienvenue a bord"));
        assertTrue(message.htmlBody().contains("Decouvrir mes voyages"));
        assertTrue(message.textBody().contains("http://localhost:5173/traveler/trips"));
    }
}