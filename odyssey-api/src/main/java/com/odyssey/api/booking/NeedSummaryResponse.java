package com.odyssey.api.booking;

import com.odyssey.api.need.NeedStatus;
import com.odyssey.api.need.NeedType;

public record NeedSummaryResponse(
    Long id,
    NeedType type,
    NeedStatus status,
    String notes,
    FlightCriteriaResponse flightCriteria,
    AccommodationCriteriaResponse accommodationCriteria
) {}