package com.odyssey.api.event;

public record QuoteRejectedEvent(
    Long quoteId,
    Long bookingRequestId,
    Long travelerId,
    Long agentId
) {}