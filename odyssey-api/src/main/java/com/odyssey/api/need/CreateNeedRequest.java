package com.odyssey.api.need;

import com.odyssey.api.need.flight.FlightCriteriaRequest;
import com.odyssey.api.need.accommodation.AccommodationCriteriaRequest;

import jakarta.validation.constraints.NotNull;

public record CreateNeedRequest(

    @NotNull
    NeedType type,

    String notes,

    @NotNull
    Long tripId
    ,

    FlightCriteriaRequest flightCriteria,
    AccommodationCriteriaRequest accommodationCriteria

) {}