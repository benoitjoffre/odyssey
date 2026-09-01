package com.odyssey.api.booking;

public record TripSummaryResponse(
    Long id,
    String title,
    String startDate,
    String endDate
) {}