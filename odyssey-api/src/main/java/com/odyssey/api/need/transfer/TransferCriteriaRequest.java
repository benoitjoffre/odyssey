package com.odyssey.api.need.transfer;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record TransferCriteriaRequest(
    @NotBlank String pickupLocation,
    @NotBlank String dropoffLocation,
    @Min(1) int travelers
) {}
