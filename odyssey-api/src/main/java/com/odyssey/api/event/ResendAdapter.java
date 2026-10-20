package com.odyssey.api.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.core.net.RequestOptions;
import com.resend.services.emails.model.CreateEmailOptions;

import java.util.Objects;
import java.net.URI;
import java.util.Locale;

@Service 
public class ResendAdapter implements EmailService {

    private static final Logger logger =
        LoggerFactory.getLogger(ResendAdapter.class);

    private final Resend resend;
    private final String fromAddress;
    private final boolean enabled;
    private final String replyTo;

    public ResendAdapter(
        @Value("${resend.api-key:}") String apiKey,
        @Value("${resend.from:Odyssey <noreply@gocuback.com>}") String fromAddress,
        @Value("${resend.reply-to:}") String replyTo,
        @Value("${app.email.enabled:false}") boolean deliveryEnabled,
        @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl
    ) {
        this.fromAddress = fromAddress;
        this.replyTo = replyTo;
        this.enabled = deliveryEnabled && apiKey != null && !apiKey.isBlank();
        URI frontend = URI.create(frontendBaseUrl);
        String host = frontend.getHost() == null ? "" : frontend.getHost().toLowerCase(Locale.ROOT);
        if (deliveryEnabled && (!enabled || fromAddress.isBlank()
            || !"https".equals(frontend.getScheme()) || host.isBlank()
            || host.equals("localhost") || host.endsWith(".localhost") || host.startsWith("127.")
            || host.equals("[::1]") || host.equals("0.0.0.0"))) {
            throw new IllegalStateException("Email delivery requires RESEND_API_KEY, RESEND_FROM and a public HTTPS FRONTEND_BASE_URL");
        }
        this.resend = enabled ? new Resend(apiKey) : null;

        if (!enabled) {
            logger.warn(
                "Resend delivery is disabled; no network sends are allowed"
            );
        }
    }

    @Override
    public String sendEmail(EmailMessage message, String idempotencyKey) {
        Objects.requireNonNull(message, "Email message cannot be null");

        if (!enabled) {
            throw new IllegalStateException("Email delivery is disabled or the Resend API key is missing");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Email delivery requires an idempotency key");
        }

        CreateEmailOptions.Builder builder = CreateEmailOptions.builder()
            .from(fromAddress)
            .to(message.to())
            .subject(message.subject())
            .html(message.htmlBody());

        if (message.textBody() != null && !message.textBody().isBlank()) {
            builder.text(message.textBody());
        }
        if (!replyTo.isBlank()) {
            builder.replyTo(replyTo);
        }

        CreateEmailOptions params = builder.build();

        try {
            var response = resend.emails().send(params,
                RequestOptions.builder().setIdempotencyKey(idempotencyKey).build());
            if (response == null || response.getId() == null || response.getId().isBlank()) {
                throw new IllegalStateException("Resend did not return an email identifier");
            }
            return response.getId();
        } catch (ResendException exception) {
            throw new EmailDeliveryException(
                "Failed to deliver email through Resend",
                exception
            );
        }
    }
}
