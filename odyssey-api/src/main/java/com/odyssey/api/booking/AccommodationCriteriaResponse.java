package com.odyssey.api.booking;

public record AccommodationCriteriaResponse(
    String city,
    int travelers,
    int rooms
) {}