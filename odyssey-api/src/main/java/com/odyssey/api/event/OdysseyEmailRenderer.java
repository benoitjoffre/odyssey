package com.odyssey.api.event;

import java.math.BigDecimal;
import java.net.URI;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OdysseyEmailRenderer {

    private final String frontendBaseUrl;

    public OdysseyEmailRenderer(
        @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl
    ) {
        URI uri = URI.create(frontendBaseUrl);
        if (uri.getHost() == null || !("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
            || uri.getRawQuery() != null || uri.getRawFragment() != null || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("FRONTEND_BASE_URL must be an absolute HTTP(S) URL without credentials, query or fragment");
        }
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    public EmailMessage agentBookingRequested(String to, String firstName, Long requestId,
        String travelerFirstName, String tripTitle, String needLabel) {
        return message(to, Language.FR, firstName,
            "Nouvelle demande à traiter", "Nouvelle demande pour " + safe(tripTitle),
            safe(travelerFirstName) + " a ajouté " + safe(needLabel) + " au voyage « " + safe(tripTitle)
                + " ». Prenez en charge la demande pour préparer une proposition.",
            "Ouvrir la demande", "/agent/booking-requests/" + requestId, "Demande #" + requestId);
    }

    public EmailMessage agentQuoteAccepted(String to, String firstName, Long requestId, String travelerFirstName) {
        return message(to, Language.FR, firstName,
            "Proposition acceptée", "Proposition acceptée pour la demande #" + requestId,
            safe(travelerFirstName) + " a accepté votre proposition. Consultez la demande pour organiser la suite.",
            "Voir la demande", "/agent/booking-requests/" + requestId, "Demande #" + requestId);
    }

    public EmailMessage agentQuoteRejected(String to, String firstName, Long requestId, String travelerFirstName) {
        return message(to, Language.FR, firstName,
            "Proposition refusée", "Proposition refusée pour la demande #" + requestId,
            safe(travelerFirstName) + " a refusé votre proposition. Consultez la demande pour préparer une alternative.",
            "Voir la demande", "/agent/booking-requests/" + requestId, "Demande #" + requestId);
    }

    public EmailMessage agentPaymentSucceeded(String to, String firstName, Long tripId,
        BigDecimal fee, String currency) {
        return message(to, Language.FR, firstName,
            "Frais d’assistance payés", "Paiement Odyssey confirmé pour le voyage #" + tripId,
            "Les frais d’assistance Odyssey de " + money(fee, currency, Language.FR)
                + " ont été payés et enregistrés pour ce voyage. Ce règlement est distinct des paiements aux fournisseurs.",
            "Voir les demandes", "/agent/booking-requests", "Voyage #" + tripId);
    }

    public EmailMessage travelerBookingRequested(String to, String language, String firstName, Long requestId,
        Long tripId, String tripTitle) {
        Language lang = Language.resolve(language);
        String title = lang.pick("Demande reçue", "Request received", "Solicitud recibida");
        return message(to, lang, firstName, title, title + " · " + safe(tripTitle),
            lang.pick(
                "Votre demande a bien été reçue. Un conseiller va l’étudier pour vous accompagner dans votre voyage.",
                "We have received your request. An advisor will review it and help you plan your trip.",
                "Hemos recibido tu solicitud. Un asesor la revisará para ayudarte a organizar tu viaje."),
            lang.pick("Suivre mon voyage", "View my trip", "Ver mi viaje"), "/traveler/trips/" + tripId,
            safe(tripTitle) + " · " + lang.pick("Demande", "Request", "Solicitud") + " #" + requestId);
    }

    public EmailMessage travelerBookingAssigned(String to, String language, String firstName, Long requestId,
        String agentFirstName, Long tripId, String tripTitle) {
        Language lang = Language.resolve(language);
        String title = lang.pick("Votre conseiller vous accompagne", "Your advisor is here", "Tu asesor te acompaña");
        return message(to, lang, firstName, title, title + " · " + safe(tripTitle),
            safe(agentFirstName) + lang.pick(
                " commence à vous accompagner dans l’organisation de votre voyage. Retrouvez le suivi de chaque demande dans votre espace. Vous recevrez un mail lorsque des propositions seront disponibles.",
                " is starting to help you plan your trip. Track each request in your account. We will email you when offers are available.",
                " empieza a ayudarte a organizar tu viaje. Consulta el seguimiento de cada solicitud en tu cuenta. Te avisaremos por correo cuando haya propuestas disponibles."),
            lang.pick("Suivre mon voyage", "View my trip", "Ver mi viaje"), "/traveler/trips/" + tripId,
            safe(tripTitle));
    }

    public EmailMessage travelerQuoteSent(String to, String language, String firstName, Long requestId,
        BigDecimal amount, String currency, String description, Long tripId, String tripTitle) {
        Language lang = Language.resolve(language);
        String title = lang.pick("Nouvelle proposition", "New offer", "Nueva propuesta");
        return message(to, lang, firstName, title, title + " · " + safe(tripTitle),
            lang.pick("Votre conseiller vous propose une offre à ", "Your advisor has prepared an offer for ",
                "Tu asesor ha preparado una oferta de ") + money(amount, currency, lang) + ". " + safe(description),
            lang.pick("Voir la proposition", "Review offer", "Ver propuesta"), "/traveler/trips/" + tripId,
            safe(tripTitle) + " · " + lang.pick("Demande", "Request", "Solicitud") + " #" + requestId);
    }

    public EmailMessage travelerTripQuotesSent(String to, String language, String firstName, String tripTitle,
        int count, Long tripId) {
        if (count < 1) {
            throw new IllegalArgumentException("A quote email requires at least one quote");
        }
        Language lang = Language.resolve(language);
        String title = count == 1
            ? lang.pick("Votre proposition est prête", "Your offer is ready", "Tu propuesta está lista")
            : lang.pick("Vos propositions sont prêtes", "Your offers are ready", "Tus propuestas están listas");
        return message(to, lang, firstName, title, title + " · " + safe(tripTitle),
            count + (count == 1
                ? lang.pick(" proposition est disponible pour votre voyage « ", " offer is available for your trip “",
                    " propuesta está disponible para tu viaje « ")
                : lang.pick(" propositions sont disponibles pour votre voyage « ", " offers are available for your trip “",
                    " propuestas están disponibles para tu viaje « "))
                + safe(tripTitle) + lang.pick(" ». Consultez les détails pour faire votre choix.",
                    "”. Review the details to make your choice.", " ». Consulta los detalles para elegir."),
            lang.pick("Ouvrir le voyage", "Open trip", "Abrir viaje"), "/traveler/trips/" + tripId, tripTitle);
    }

    public EmailMessage travelerOnboardingCompleted(String to, String language, String firstName) {
        Language lang = Language.resolve(language);
        return message(to, lang, firstName,
            lang.pick("Bienvenue à bord", "Welcome aboard", "Bienvenido a bordo"),
            lang.pick("Bienvenue chez Odyssey", "Welcome to Odyssey", "Bienvenido a Odyssey"),
            lang.pick("Votre profil est maintenant complet. Découvrez des expériences et commencez à organiser votre prochain voyage.",
                "Your profile is complete. Discover experiences and start planning your next trip.",
                "Tu perfil está completo. Descubre experiencias y empieza a organizar tu próximo viaje."),
            lang.pick("Découvrir mes voyages", "Explore my trips", "Explorar mis viajes"), "/traveler/trips",
            lang.pick("Votre espace voyageur", "Your traveler account", "Tu espacio de viajero"));
    }

    public EmailMessage travelerPaymentSucceeded(String to, String language, String firstName,
        Long tripId, String tripTitle, BigDecimal fee, String currency) {
        Language lang = Language.resolve(language);
        String title = lang.pick("Vos frais d’assistance sont payés", "Your assistance fee is paid",
            "Tu tarifa de asistencia está pagada");
        return message(to, lang, firstName, title, title + " · " + safe(tripTitle),
            lang.pick("Nous avons reçu ", "We have received ", "Hemos recibido ") + money(fee, currency, lang)
                + lang.pick(
                    " pour l’assistance Odyssey de votre voyage. Ce paiement ne règle pas les prestations des fournisseurs et ne confirme pas leurs réservations. Votre conseiller vous accompagne pour la suite.",
                    " for your trip’s Odyssey assistance. This does not pay suppliers or confirm their bookings. Your advisor will help with the next steps.",
                    " por la asistencia Odyssey de tu viaje. Este pago no abona los servicios de los proveedores ni confirma sus reservas. Tu asesor te acompañará en los próximos pasos."),
            lang.pick("Suivre mon voyage", "View my trip", "Ver mi viaje"), "/traveler/trips/" + tripId, tripTitle);
    }

    public EmailMessage travelerBookingConfirmed(String to, String language, String firstName,
        Long tripId, String tripTitle, String providerReference) {
        Language lang = Language.resolve(language);
        String title = lang.pick("Réservation confirmée", "Booking confirmed", "Reserva confirmada");
        return message(to, lang, firstName, title, title + " · " + safe(tripTitle),
            lang.pick("Votre conseiller a confirmé une réservation pour votre voyage. Référence fournisseur : ",
                "Your advisor has confirmed a booking for your trip. Supplier reference: ",
                "Tu asesor ha confirmado una reserva para tu viaje. Referencia del proveedor: ")
                + safe(providerReference) + lang.pick(
                    ". Consultez votre voyage pour les détails et les éventuelles étapes restantes.",
                    ". Check your trip for details and any remaining steps.",
                    ". Consulta tu viaje para ver los detalles y los próximos pasos."),
            lang.pick("Voir ma réservation", "View my booking", "Ver mi reserva"), "/traveler/trips/" + tripId, tripTitle);
    }

    public EmailMessage travelerProviderPaymentRequired(String to, String language, String firstName,
        Long tripId, String tripTitle) {
        Language lang = Language.resolve(language);
        String title = lang.pick("Instructions de paiement disponibles", "Payment instructions available", "Instrucciones de pago disponibles");
        return message(to, lang, firstName, title, title + " · " + safe(tripTitle),
            lang.pick(
                "Votre conseiller a ajouté ou actualisé les instructions de paiement d’une réservation. Vérifiez le statut actuel dans votre espace Odyssey avant tout règlement au fournisseur. Ce paiement est distinct des frais d’assistance Odyssey.",
                "Your advisor has added or updated payment instructions for a booking. Check its current status in your Odyssey account before paying the supplier. This payment is separate from the Odyssey assistance fee.",
                "Tu asesor ha añadido o actualizado las instrucciones de pago de una reserva. Comprueba su estado actual en tu cuenta Odyssey antes de pagar al proveedor. Este pago es independiente de la tarifa de asistencia Odyssey."),
            lang.pick("Consulter les instructions", "View instructions", "Ver instrucciones"),
            "/traveler/trips/" + tripId, tripTitle);
    }

    private EmailMessage message(String to, Language language, String firstName, String title,
        String subject, String summary, String cta, String path, String reference) {
        String url = frontendBaseUrl + path;
        String intro = language.pick("Bonjour ", "Hi ", "Hola ") + safe(firstName) + ",";
        String footer = language.pick("Vous recevez ce mail pour le suivi de votre compte ou de votre voyage Odyssey.",
            "You received this email about your Odyssey account or trip.",
            "Recibes este correo sobre tu cuenta o tu viaje Odyssey.");
        String fallback = language.pick("Si le bouton ne fonctionne pas, ouvrez ce lien :",
            "If the button does not work, open this link:", "Si el botón no funciona, abre este enlace:");
        String html = """
            <!doctype html>
            <html lang="%s">
            <head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
            <meta name="color-scheme" content="light"><title>%s</title></head>
            <body style="margin:0;padding:0;background:#f7f9f8;color:#102a32;font-family:'Avenir Next','Segoe UI',Arial,sans-serif;">
              <div style="display:none;font-size:1px;line-height:1px;max-height:0;max-width:0;opacity:0;overflow:hidden;mso-hide:all;">%s</div>
              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f7f9f8;">
                <tr><td align="center" style="padding:24px 12px;">
                  <!--[if mso]><table role="presentation" width="600"><tr><td><![endif]-->
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:600px;table-layout:fixed;overflow-wrap:anywhere;word-break:break-word;background:#ffffff;border:1px solid #e1e8e6;border-radius:18px;">
                    <tr><td style="padding:28px 24px 20px;border-bottom:1px solid #e1e8e6;">
                      <a href="%s" style="text-decoration:none;color:#0866a5;font-size:28px;font-weight:800;"><img src="%s/email/odyssey-bird.png" width="40" height="40" alt="" style="display:inline-block;vertical-align:middle;border:0;margin-right:10px;">Odyssey</a>
                      <div style="margin-top:8px;height:3px;width:48px;background:#ffc515;"></div>
                    </td></tr>
                    <tr><td style="padding:28px 24px;">
                      <h1 style="margin:0 0 24px;font-size:26px;line-height:1.25;color:#102a32;">%s</h1>
                      <p style="margin:0 0 12px;font-size:16px;line-height:1.6;">%s</p>
                      <p style="margin:0 0 24px;font-size:16px;line-height:1.65;color:#425b61;">%s</p>
                      <table role="presentation" cellpadding="0" cellspacing="0"><tr>
                        <td bgcolor="#0866a5" style="border-radius:10px;mso-padding-alt:14px 22px;">
                          <a href="%s" style="display:inline-block;padding:14px 22px;color:#ffffff;font-size:16px;font-weight:700;text-decoration:none;line-height:1.4;">%s</a>
                        </td></tr></table>
                      <p style="margin:24px 0 0;padding:14px;background:#f7f9f8;border-radius:10px;color:#425b61;font-size:14px;line-height:1.5;">%s</p>
                      <p style="margin:24px 0 0;font-size:12px;line-height:1.6;color:#425b61;">%s<br>
                        <a href="%s" style="color:#0866a5;word-break:break-all;">%s</a>
                      </p>
                    </td></tr>
                    <tr><td style="padding:20px 24px;border-top:1px solid #e1e8e6;color:#425b61;font-size:12px;line-height:1.6;">%s</td></tr>
                  </table>
                  <!--[if mso]></td></tr></table><![endif]-->
                </td></tr>
              </table>
            </body></html>
            """.formatted(language.code, escape(subject), escape(summary), escape(frontendBaseUrl), escape(frontendBaseUrl),
                escape(title), escape(intro), escape(summary), escape(url), escape(cta), escape(safe(reference)),
                escape(fallback), escape(url), escape(url), escape(footer));
        return new EmailMessage(to, subject, html, intro + "\n\n" + summary + "\n\n" + cta + ": " + url
            + "\n\n" + safe(reference) + "\n\n" + footer);
    }

    private String money(BigDecimal amount, String currency, Language language) {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag(language.code));
        format.setCurrency(Currency.getInstance(currency));
        return format.format(amount);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private enum Language {
        FR("fr"), EN("en"), ES("es");

        private final String code;

        Language(String code) {
            this.code = code;
        }

        String pick(String fr, String en, String es) {
            return switch (this) {
                case FR -> fr;
                case EN -> en;
                case ES -> es;
            };
        }

        static Language resolve(String value) {
            String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT).replace('_', '-');
            return switch (normalized.split("-")[0]) {
                case "en" -> EN;
                case "es" -> ES;
                default -> FR;
            };
        }
    }
}
