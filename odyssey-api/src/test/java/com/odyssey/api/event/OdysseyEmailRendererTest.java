package com.odyssey.api.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
        assertTrue(message.htmlBody().contains("Bienvenue à bord"));
        assertTrue(message.htmlBody().contains("Découvrir mes voyages"));
        assertTrue(message.textBody().contains("http://localhost:5173/traveler/trips"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"fr", "en", "es"})
    void allTravelerTemplatesHaveLocalizedAccessibleLayoutAndWorkingTripLinks(String language) throws Exception {
        var messages = List.of(
            renderer.travelerBookingRequested("a@example.com", language, "Alice", 7L, 42L, "Lisbonne"),
            renderer.travelerBookingAssigned("a@example.com", language, "Alice", 7L, "Paul", 42L, "Lisbonne"),
            renderer.travelerQuoteSent("a@example.com", language, "Alice", 7L,
                new BigDecimal("1234.50"), "EUR", "Vol direct", 42L, "Lisbonne"),
            renderer.travelerTripQuotesSent("a@example.com", language, "Alice", "Lisbonne", 2, 42L),
            renderer.travelerPaymentSucceeded("a@example.com", language, "Alice", 42L,
                "Lisbonne", new BigDecimal("25"), "EUR"),
            renderer.travelerTripBookingsConfirmed("a@example.com", language, "Alice", 42L, "Lisbonne", 3),
            renderer.travelerProviderPaymentRequired("a@example.com", language, "Alice", 42L, "Lisbonne")
        );
        for (EmailMessage message : messages) {
            assertTrue(message.htmlBody().contains("<html lang=\"" + language + "\">"));
            assertTrue(message.htmlBody().contains("#0866a5"));
            assertTrue(message.htmlBody().contains("/email/odyssey-bird.png"));
            assertTrue(message.htmlBody().contains("max-width:600px"));
            assertTrue(message.htmlBody().contains("overflow-wrap:anywhere"));
            assertFalse(message.htmlBody().contains("style=\"width:600px"));
            assertTrue(message.htmlBody().contains("http://localhost:5173/traveler/trips/42"));
            assertTrue(message.textBody().contains("http://localhost:5173/traveler/trips/42"));
            assertFalse(message.htmlBody().contains("/traveler/bookings"));
            assertFalse(message.subject().contains("(s)"));
            if (!language.equals("fr")) {
                assertFalse(message.htmlBody().contains("Vous recevez"));
            }
        }
        Path previews = Path.of("target", "email-previews");
        Files.createDirectories(previews);
        Files.writeString(previews.resolve("payment-" + language + ".html"), messages.get(4).htmlBody());
    }

    @Test
    void escapesDynamicHtmlButKeepsPlainTextReadable() {
        EmailMessage message = renderer.travelerTripBookingsConfirmed("a@example.com", "fr", "<Alice>",
            42L, "Cuba & soleil\"><script>alert(1)</script>", 1);
        assertFalse(message.htmlBody().contains("<script>"));
        assertTrue(message.htmlBody().contains("&lt;Alice&gt;"));
        assertTrue(message.htmlBody().contains("Cuba &amp; soleil"));
        assertTrue(message.textBody().contains("Cuba & soleil"));
    }

    @Test
    void normalizesLocalesAndFormatsMoneyAndPlural() {
        assertTrue(renderer.travelerOnboardingCompleted("a@example.com", "ES_mx", "Alice")
            .htmlBody().contains("<html lang=\"es\">"));
        assertTrue(renderer.travelerOnboardingCompleted("a@example.com", null, "Alice")
            .htmlBody().contains("<html lang=\"fr\">"));
        EmailMessage fr = renderer.travelerPaymentSucceeded("a@example.com", "fr", "Alice", 42L,
            "Lisbonne", new BigDecimal("25.50"), "EUR");
        assertTrue(fr.textBody().contains("25,50"));
        assertTrue(fr.textBody().contains("ne confirme pas"));
        assertTrue(renderer.travelerTripQuotesSent("a@example.com", "fr", "Alice", "Lisbonne", 1, 42L)
            .textBody().contains("1 proposition est"));
        assertThrows(IllegalArgumentException.class,
            () -> renderer.travelerTripQuotesSent("a@example.com", "fr", "Alice", "Lisbonne", 0, 42L));
        assertTrue(renderer.travelerTripBookingsConfirmed("a@example.com", "fr", "Alice", 42L, "Lisbonne", 1)
            .textBody().contains("la réservation de votre voyage"));
        assertTrue(renderer.travelerTripBookingsConfirmed("a@example.com", "fr", "Alice", 42L, "Lisbonne", 3)
            .textBody().contains("les 3 réservations"));
        assertThrows(IllegalArgumentException.class,
            () -> renderer.travelerTripBookingsConfirmed("a@example.com", "fr", "Alice", 42L, "Lisbonne", 0));
    }

    @Test
    void allAgentTemplatesUseExistingRoutes() {
        var messages = List.of(
            renderer.agentBookingRequested("a@example.com", "Paul", 7L, "Alice", "Lisbonne", "un vol"),
            renderer.agentQuoteAccepted("a@example.com", "Paul", 7L, "Alice"),
            renderer.agentQuoteRejected("a@example.com", "Paul", 7L, "Alice"),
            renderer.agentPaymentSucceeded("a@example.com", "Paul", 42L, BigDecimal.TEN, "EUR")
        );
        for (EmailMessage message : messages) {
            assertTrue(message.htmlBody().contains("/agent/booking-requests"));
            assertFalse(message.htmlBody().contains("/agent/trips/"));
            assertTrue(message.textBody().contains("/agent/booking-requests"));
        }
    }

    @Test
    void rejectsInvalidBaseUrlsAndStripsTrailingSlashes() {
        assertThrows(IllegalArgumentException.class, () -> new OdysseyEmailRenderer("javascript:alert(1)"));
        assertThrows(IllegalArgumentException.class, () -> new OdysseyEmailRenderer("https://example.com/?x=1"));
        assertTrue(new OdysseyEmailRenderer("https://example.com///")
            .travelerOnboardingCompleted("a@example.com", "fr", "Alice")
            .textBody().contains("https://example.com/traveler/trips"));
    }
}