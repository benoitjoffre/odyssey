package com.odyssey.api.event;

public record QuoteAcceptedEvent(
        Long quoteId,
        Long bookingRequestId,
        Long travelerId,
        Long agentId
) {}