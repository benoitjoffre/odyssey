package com.odyssey.api.event;

public record EmailMessage(
    String to,
    String subject,
    String htmlBody,
    String textBody
) {}