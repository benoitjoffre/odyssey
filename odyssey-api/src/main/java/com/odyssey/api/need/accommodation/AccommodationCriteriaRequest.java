package com.odyssey.api.need.accommodation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AccommodationCriteriaRequest(

        @NotBlank
        String city,

        @Min(1)
        int travelers,

        @Min(1)
        int rooms
) {
}