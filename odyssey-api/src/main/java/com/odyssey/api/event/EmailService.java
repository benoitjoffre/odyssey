package com.odyssey.api.event;

public interface EmailService {

    void sendEmail(EmailMessage message);

    default void sendEmail(String to, String subject, String htmlBody) {
        sendEmail(new EmailMessage(to, subject, htmlBody, null));
    }
}
