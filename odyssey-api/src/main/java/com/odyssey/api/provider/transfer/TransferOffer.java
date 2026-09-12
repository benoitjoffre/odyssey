package com.odyssey.api.provider.transfer;

import com.odyssey.api.provider.ProviderOffer;

import java.math.BigDecimal;

public record TransferOffer(
        String provider,
        String externalId,
        String vehicleType,
        BigDecimal price,
        String currency,
        int travelers
) implements ProviderOffer {
}