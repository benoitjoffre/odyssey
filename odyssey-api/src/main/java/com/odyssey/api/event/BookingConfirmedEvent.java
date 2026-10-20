package com.odyssey.api.event;

public record BookingConfirmedEvent(
    Long bookingId, Long travelerId, Long tripId, String tripTitle, String providerReference
) {}
