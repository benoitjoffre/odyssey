package com.odyssey.api.need.flight;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record FlightCriteriaRequest(

        @NotBlank
        String origin,

        @NotBlank
        String destination,

        @Min(1)
        int travelers
) {
}