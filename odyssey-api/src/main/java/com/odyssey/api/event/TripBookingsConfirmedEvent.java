package com.odyssey.api.event;

public record TripBookingsConfirmedEvent(
    Long tripId, Long travelerId, String tripTitle, int bookingCount
) {}
