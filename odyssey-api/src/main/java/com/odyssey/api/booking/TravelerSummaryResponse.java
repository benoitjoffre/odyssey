package com.odyssey.api.booking;

public record TravelerSummaryResponse(
    Long id,
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    String whatsappNumber
) {}