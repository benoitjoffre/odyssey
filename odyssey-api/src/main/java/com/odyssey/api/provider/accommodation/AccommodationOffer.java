package com.odyssey.api.provider.accommodation;

import com.odyssey.api.provider.ProviderOffer;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccommodationOffer(
        String provider,
        String externalId,
        BigDecimal price,
        String currency,
        String hotelName,
        String city,
        LocalDate checkIn,
        LocalDate checkOut,
        String roomType
) implements ProviderOffer {
}