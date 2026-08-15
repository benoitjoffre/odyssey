package com.odyssey.api.booking;

import jakarta.validation.constraints.NotNull;

public record CreateBookingRequest(
    @NotNull Long needId,
    String notes
) {
}