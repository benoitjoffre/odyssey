package com.odyssey.api.event;

import java.math.BigDecimal;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OdysseyEmailRenderer {

    private final String frontendBaseUrl;

    public OdysseyEmailRenderer(
        @Value("${app.frontend.base-url:http://localhost:5173}")
        String frontendBaseUrl
    ) {
        this.frontendBaseUrl = stripTrailingSlash(frontendBaseUrl);
    }

    public EmailMessage agentBookingRequested(
        String to,
        String agentFirstName,
        Long bookingRequestId,
        String travelerFirstName,
        String tripTitle,
        String needLabel
    ) {
        String subject = "Nouvelle demande #" + bookingRequestId + " a traiter";
        String actionUrl = frontendBaseUrl + "/agent/booking-requests/" + bookingRequestId;
        String intro = "Bonjour " + safe(agentFirstName) + ",";
        String summary = safe(travelerFirstName)
            + " a ajoute "
            + safe(needLabel)
            + " pour le voyage \""
            + safe(tripTitle)
            + "\".";
        String html = layout(
            "Nouvelle demande de reservation",
            intro,
            summary,
            "Ouvrir la demande",
            actionUrl,
            "Booking request #" + bookingRequestId
        );
        String text = intro + " " + summary + " Ouvrir: " + actionUrl;
        return new EmailMessage(to, subject, html, text);
    }

    public EmailMessage agentQuoteAccepted(
        String to,
        String agentFirstName,
        Long bookingRequestId,
        String travelerFirstName
    ) {
        String subject = "Proposition acceptee pour la demande #" + bookingRequestId;
        String actionUrl = frontendBaseUrl + "/agent/booking-requests/" + bookingRequestId;
        String intro = "Bonjour " + safe(agentFirstName) + ",";
        String summary = safe(travelerFirstName)
            + " a accepte votre proposition pour la demande #"
            + bookingRequestId
            + ".";
        String html = layout(
            "Proposition acceptee",
            intro,
            summary,
            "Voir la demande",
            actionUrl,
            "Booking request #" + bookingRequestId
        );
        String text = intro + " " + summary + " Ouvrir: " + actionUrl;
        return new EmailMessage(to, subject, html, text);
    }

    public EmailMessage agentPaymentSucceeded(
        String to,
        String agentFirstName,
        Long tripId,
        BigDecimal assistanceFee,
        String currency
    ) {
        String subject = "Paiement Odyssey confirme pour le voyage #" + tripId;
        String actionUrl = frontendBaseUrl + "/agent/trips/" + tripId;
        String intro = "Bonjour " + safe(agentFirstName) + ",";
        String summary = "Les frais d'assistance Odyssey ("
            + assistanceFee
            + " "
            + safe(currency)
            + ") ont ete confirmes pour le voyage #"
            + tripId
            + ".";
        String html = layout(
            "Paiement confirme",
            intro,
            summary,
            "Suivre le voyage",
            actionUrl,
            "Trip #" + tripId
        );
        String text = intro + " " + summary + " Ouvrir: " + actionUrl;
        return new EmailMessage(to, subject, html, text);
    }

    public EmailMessage travelerBookingRequested(
        String to,
        String preferredLanguage,
        String travelerFirstName,
        Long bookingRequestId
    ) {
        Language language = resolveLanguage(preferredLanguage);
        String actionUrl = frontendBaseUrl + "/traveler/bookings";

        if (language == Language.EN) {
            String subject = "Your request #" + bookingRequestId + " was received";
            String intro = "Hi " + safe(travelerFirstName) + ",";
            String summary = "your booking request #" + bookingRequestId + " has been received by Odyssey.";
            String html = layout("Request received", intro, summary, "Track request", actionUrl, "Request #" + bookingRequestId);
            String text = intro + " " + summary + " Open: " + actionUrl;
            return new EmailMessage(to, subject, html, text);
        }

        if (language == Language.ES) {
            String subject = "Tu solicitud #" + bookingRequestId + " fue recibida";
            String intro = "Hola " + safe(travelerFirstName) + ",";
            String summary = "tu solicitud de reserva #" + bookingRequestId + " ha sido recibida por Odyssey.";
            String html = layout("Solicitud recibida", intro, summary, "Seguir solicitud", actionUrl, "Solicitud #" + bookingRequestId);
            String text = intro + " " + summary + " Abrir: " + actionUrl;
            return new EmailMessage(to, subject, html, text);
        }

        String subject = "Votre demande #" + bookingRequestId + " a bien ete recue";
        String intro = "Bonjour " + safe(travelerFirstName) + ",";
        String summary = "votre demande de reservation #" + bookingRequestId + " a bien ete recue par Odyssey.";
        String html = layout("Demande recue", intro, summary, "Suivre la demande", actionUrl, "Demande #" + bookingRequestId);
        String text = intro + " " + summary + " Ouvrir: " + actionUrl;
        return new EmailMessage(to, subject, html, text);
    }

    public EmailMessage travelerBookingAssigned(
        String to,
        String preferredLanguage,
        String travelerFirstName,
        Long bookingRequestId,
        String agentFirstName
    ) {
        Language language = resolveLanguage(preferredLanguage);
        String actionUrl = frontendBaseUrl + "/traveler/bookings";

        if (language == Language.EN) {
            String subject = "An agent is handling request #" + bookingRequestId;
            String intro = "Hi " + safe(travelerFirstName) + ",";
            String summary = safe(agentFirstName) + " is now handling your request #" + bookingRequestId + ".";
            String html = layout("Agent assigned", intro, summary, "View details", actionUrl, "Request #" + bookingRequestId);
            String text = intro + " " + summary + " Open: " + actionUrl;
            return new EmailMessage(to, subject, html, text);
        }

        if (language == Language.ES) {
            String subject = "Un agente esta gestionando la solicitud #" + bookingRequestId;
            String intro = "Hola " + safe(travelerFirstName) + ",";
            String summary = safe(agentFirstName) + " ahora esta gestionando tu solicitud #" + bookingRequestId + ".";
            String html = layout("Agente asignado", intro, summary, "Ver detalles", actionUrl, "Solicitud #" + bookingRequestId);
            String text = intro + " " + summary + " Abrir: " + actionUrl;
            return new EmailMessage(to, subject, html, text);
        }

        String subject = "Un agent prend en charge la demande #" + bookingRequestId;
        String intro = "Bonjour " + safe(travelerFirstName) + ",";
        String summary = safe(agentFirstName) + " prend maintenant en charge votre demande #" + bookingRequestId + ".";
        String html = layout("Agent assigne", intro, summary, "Voir le suivi", actionUrl, "Demande #" + bookingRequestId);
        String text = intro + " " + summary + " Ouvrir: " + actionUrl;
        return new EmailMessage(to, subject, html, text);
    }

    public EmailMessage travelerQuoteSent(
        String to,
        String preferredLanguage,
        String travelerFirstName,
        Long bookingRequestId,
        BigDecimal totalAmount,
        String currency,
        String description
    ) {
        Language language = resolveLanguage(preferredLanguage);
        String actionUrl = frontendBaseUrl + "/traveler/quotes";
        String price = totalAmount + " " + safe(currency);

        if (language == Language.EN) {
            String subject = "New quote available for request #" + bookingRequestId;
            String intro = "Hi " + safe(travelerFirstName) + ",";
            String summary = "a new quote is available for request #" + bookingRequestId
                + " (" + price + "). " + safe(description);
            String html = layout("New quote available", intro, summary, "Review quote", actionUrl, "Request #" + bookingRequestId);
            String text = intro + " " + summary + " Open: " + actionUrl;
            return new EmailMessage(to, subject, html, text);
        }

        if (language == Language.ES) {
            String subject = "Nueva propuesta disponible para la solicitud #" + bookingRequestId;
            String intro = "Hola " + safe(travelerFirstName) + ",";
            String summary = "hay una nueva propuesta para la solicitud #" + bookingRequestId
                + " (" + price + "). " + safe(description);
            String html = layout("Nueva propuesta", intro, summary, "Revisar propuesta", actionUrl, "Solicitud #" + bookingRequestId);
            String text = intro + " " + summary + " Abrir: " + actionUrl;
            return new EmailMessage(to, subject, html, text);
        }

        String subject = "Nouvelle proposition disponible pour la demande #" + bookingRequestId;
        String intro = "Bonjour " + safe(travelerFirstName) + ",";
        String summary = "une nouvelle proposition est disponible pour la demande #" + bookingRequestId
            + " (" + price + "). " + safe(description);
        String html = layout("Nouvelle proposition", intro, summary, "Voir la proposition", actionUrl, "Demande #" + bookingRequestId);
        String text = intro + " " + summary + " Ouvrir: " + actionUrl;
        return new EmailMessage(to, subject, html, text);
    }

    public EmailMessage travelerTripQuotesSent(
        String to,
        String preferredLanguage,
        String travelerFirstName,
        String tripTitle,
        int quoteCount,
        Long tripId
    ) {
        Language language = resolveLanguage(preferredLanguage);
        String actionUrl = frontendBaseUrl + "/traveler/trips/" + tripId;

        if (language == Language.EN) {
            String subject = quoteCount + " new quote(s) for your trip";
            String intro = "Hi " + safe(travelerFirstName) + ",";
            String summary = quoteCount + " quote(s) are now available for your trip \"" + safe(tripTitle) + "\".";
            String html = layout("Trip offers are ready", intro, summary, "Open trip", actionUrl, safe(tripTitle));
            String text = intro + " " + summary + " Open: " + actionUrl;
            return new EmailMessage(to, subject, html, text);
        }

        if (language == Language.ES) {
            String subject = quoteCount + " nueva(s) propuesta(s) para tu viaje";
            String intro = "Hola " + safe(travelerFirstName) + ",";
            String summary = quoteCount + " propuesta(s) estan disponibles para tu viaje \"" + safe(tripTitle) + "\".";
            String html = layout("Ofertas del viaje listas", intro, summary, "Abrir viaje", actionUrl, safe(tripTitle));
            String text = intro + " " + summary + " Abrir: " + actionUrl;
            return new EmailMessage(to, subject, html, text);
        }

        String subject = quoteCount + " nouvelle(s) proposition(s) pour votre voyage";
        String intro = "Bonjour " + safe(travelerFirstName) + ",";
        String summary = quoteCount + " proposition(s) sont disponibles pour votre voyage \"" + safe(tripTitle) + "\".";
        String html = layout("Offres disponibles", intro, summary, "Ouvrir le voyage", actionUrl, safe(tripTitle));
        String text = intro + " " + summary + " Ouvrir: " + actionUrl;
        return new EmailMessage(to, subject, html, text);
    }

    public EmailMessage travelerOnboardingCompleted(
        String to,
        String preferredLanguage,
        String travelerFirstName
    ) {
        Language language = resolveLanguage(preferredLanguage);
        String actionUrl = frontendBaseUrl + "/traveler/trips";

        if (language == Language.EN) {
            String subject = "Welcome to Odyssey";
            String intro = "Hi " + safe(travelerFirstName) + ",";
            String summary = "your profile is now complete. You can start planning your trips with Odyssey.";
            String html = layout("Welcome aboard", intro, summary, "Explore your trips", actionUrl, "Traveler onboarding");
            String text = intro + " " + summary + " Open: " + actionUrl;
            return new EmailMessage(to, subject, html, text);
        }

        if (language == Language.ES) {
            String subject = "Bienvenido a Odyssey";
            String intro = "Hola " + safe(travelerFirstName) + ",";
            String summary = "tu perfil ya esta completo. Ya puedes empezar a planificar tus viajes con Odyssey.";
            String html = layout("Bienvenido a bordo", intro, summary, "Explorar mis viajes", actionUrl, "Onboarding viajero");
            String text = intro + " " + summary + " Abrir: " + actionUrl;
            return new EmailMessage(to, subject, html, text);
        }

        String subject = "Bienvenue chez Odyssey";
        String intro = "Bonjour " + safe(travelerFirstName) + ",";
        String summary = "votre profil est desormais complete. Vous pouvez commencer a organiser vos voyages avec Odyssey.";
        String html = layout("Bienvenue a bord", intro, summary, "Decouvrir mes voyages", actionUrl, "Onboarding voyageur");
        String text = intro + " " + summary + " Ouvrir: " + actionUrl;
        return new EmailMessage(to, subject, html, text);
    }

    private String layout(
        String title,
        String intro,
        String summary,
        String ctaLabel,
        String ctaUrl,
        String meta
    ) {
        return """
            <!doctype html>
            <html lang="en">
              <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1" />
                <title>%s</title>
              </head>
              <body style="margin:0;padding:0;background:#f4f7f5;font-family:'Avenir Next',Avenir,'Trebuchet MS',sans-serif;color:#152c2c;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f7f5;padding:24px 0;">
                  <tr>
                    <td align="center">
                      <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="width:600px;max-width:600px;background:#ffffff;border:1px solid #dce6e2;border-radius:12px;overflow:hidden;">
                        <tr>
                          <td style="background:#123f3d;padding:20px 24px;color:#ffffff;">
                            <div style="font-size:12px;letter-spacing:.08em;text-transform:uppercase;color:#b9d0cc;">Odyssey</div>
                            <div style="font-size:22px;font-weight:700;line-height:1.2;margin-top:6px;">%s</div>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:24px;">
                            <p style="margin:0 0 12px 0;font-size:15px;line-height:1.55;">%s</p>
                            <p style="margin:0 0 20px 0;font-size:15px;line-height:1.65;color:#3f5b58;">%s</p>

                            <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 0 18px 0;">
                              <tr>
                                <td style="background:#087f72;border-radius:8px;">
                                  <a href="%s" style="display:inline-block;padding:12px 18px;color:#ffffff;text-decoration:none;font-weight:700;font-size:14px;">%s</a>
                                </td>
                              </tr>
                            </table>

                            <div style="font-size:12px;color:#687b79;border-top:1px solid #dce6e2;padding-top:14px;">
                              Reference: %s
                            </div>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:14px 24px;background:#f8fbfa;color:#687b79;font-size:12px;line-height:1.5;">
                            Cet email transactionnel a ete envoye par Odyssey.
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>
              </body>
            </html>
            """.formatted(
            escape(title),
            escape(title),
            escape(intro),
            escape(summary),
            escape(ctaUrl),
            escape(ctaLabel),
            escape(meta)
        );
    }

    private Language resolveLanguage(String preferredLanguage) {
        if (preferredLanguage == null || preferredLanguage.isBlank()) {
            return Language.FR;
        }

        String normalized = preferredLanguage
            .toLowerCase(Locale.ROOT)
            .replace('-', '_');

        if (normalized.startsWith("en")) {
            return Language.EN;
        }

        if (normalized.startsWith("es")) {
            return Language.ES;
        }

        return Language.FR;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String escape(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:5173";
        }

        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }

        return value;
    }

    private enum Language {
        FR,
        EN,
        ES
    }
}