package com.odyssey.api.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;

import java.util.Objects;

@Service 
public class ResendAdapter implements EmailService {

    private static final Logger logger =
        LoggerFactory.getLogger(ResendAdapter.class);

    private final Resend resend;
    private final String fromAddress;
    private final boolean enabled;

    public ResendAdapter(
        @Value("${resend.api-key:}") String apiKey,
        @Value("${resend.from:Odyssey <benoit.joffre911@gmail.com>}")
        String fromAddress
    ) {
        this.fromAddress = fromAddress;
        this.enabled = apiKey != null && !apiKey.isBlank();
        this.resend = enabled ? new Resend(apiKey) : null;

        if (!enabled) {
            logger.warn(
                "Resend adapter is disabled because resend.api-key is empty"
            );
        }
    }

    @Override
    public void sendEmail(EmailMessage message) {
        Objects.requireNonNull(message, "Email message cannot be null");

        if (!enabled) {
            logger.info(
                "Skipping email delivery to {} because Resend is disabled",
                message.to()
            );
            return;
        }

        CreateEmailOptions.Builder builder = CreateEmailOptions.builder()
            .from(fromAddress)
            .to(message.to())
            .subject(message.subject())
            .html(message.htmlBody());

        if (message.textBody() != null && !message.textBody().isBlank()) {
            builder.text(message.textBody());
        }

        CreateEmailOptions params = builder.build();

        try {
            resend.emails().send(params);
        } catch (ResendException exception) {
            throw new EmailDeliveryException(
                "Failed to deliver email through Resend",
                exception
            );
        }
    }
}
