package com.odyssey.api.destination;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateDestinationRequest(
    @NotBlank
    String city,

    @NotBlank
    String country,

    @NotBlank
    @Size(min = 2, max = 2)
    @Pattern(regexp = "[A-Za-z]{2}")
    String countryCode
) {
}