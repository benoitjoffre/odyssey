package com.odyssey.api.trip;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTripRequest(
    @NotBlank
    String title,

    @NotNull
    LocalDate startDate,

    @NotNull
    LocalDate endDate,

    @NotNull
    Long travelerId,

    Long travelEventId
) {
}