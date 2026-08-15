package com.odyssey.api.booking;

public record BookingRequestResponse(
    Long id,
    BookingRequestStatus status,
    String notes,
    Long needId
) {
}