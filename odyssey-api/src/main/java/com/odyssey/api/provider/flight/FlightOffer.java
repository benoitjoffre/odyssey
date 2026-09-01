package com.odyssey.api.provider.flight;

import com.odyssey.api.provider.ProviderOffer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FlightOffer(
        String provider,
        String externalId,
        BigDecimal price,
        String currency,
        String origin,
        String destination,
        LocalDateTime departure,
        LocalDateTime arrival,
        String airline
) implements ProviderOffer {
}