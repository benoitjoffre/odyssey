package com.odyssey.api.event;

public record BookingRequestedEvent(
    Long bookingRequestId,
    Long needId,
    Long travelerId
) {}