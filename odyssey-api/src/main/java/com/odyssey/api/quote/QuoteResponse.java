package com.odyssey.api.quote;

import java.math.BigDecimal;
import java.time.Instant;

public record QuoteResponse(
        Long id,
        Long bookingRequestId,
        String provider,
        String externalOfferId,
        BigDecimal providerPrice,
        BigDecimal sellingPrice,
        String currency,
        String description,
        QuoteStatus status,
        Instant createdAt,
        Instant expiresAt
) {
}