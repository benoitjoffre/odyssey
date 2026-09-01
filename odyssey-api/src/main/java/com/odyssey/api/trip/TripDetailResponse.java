package com.odyssey.api.trip;

import java.time.LocalDate;
import java.util.List;

public record TripDetailResponse(
    Long id,
    String title,
    LocalDate startDate,
    LocalDate endDate,
    TripStatus status,
    Long travelerId,
    List<TripNeedResponse> needs,
    Long travelEventId
) {}