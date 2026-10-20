package com.odyssey.api.event;

public interface EmailService {

    String sendEmail(EmailMessage message, String idempotencyKey);
}
