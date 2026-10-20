package com.odyssey.api.event;

public record ProviderPaymentRequiredEvent(
    Long bookingId, Long travelerId, Long tripId, String tripTitle
) {}
