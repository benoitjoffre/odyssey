package com.odyssey.api.booking;

public record FlightCriteriaResponse(
    String origin,
    String destination,
    int travelers
) {}