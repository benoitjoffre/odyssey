package com.odyssey.api.booking.confirmation;

import java.time.Instant;

public record BookingResponse(
    Long id,
    Long quoteId,
    Long bookingRequestId,
    BookingStatus status,
    String providerConfirmationId,
    Instant createdAt,
    Instant confirmedAt
) {}