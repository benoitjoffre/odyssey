package com.odyssey.api.quote;

import java.math.BigDecimal;
import java.time.Instant;

public record TravelerQuoteResponse(
    Long id,
    Long bookingRequestId,
    BigDecimal price,
    String currency,
    String description,
    QuoteStatus status,
    Instant createdAt,
    Instant expiresAt
) {}