package com.odyssey.api.event;

public record QuoteSentEvent(
        Long quoteId,
        Long bookingRequestId,
        Long travelerId
) {
}