package com.odyssey.api.event;

public record BookingAssignedEvent(
    Long bookingRequestId,
    Long travelerId,
    Long agentId
) {}