package com.odyssey.api.quote;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateQuoteRequest(
        String provider,
        String externalOfferId,
        BigDecimal providerPrice,
        BigDecimal sellingPrice,
        String currency,
        String description,
        Instant expiresAt
) {
}