package com.odyssey.api.provider.accommodation;

import java.time.LocalDate;

public record AccommodationSearchRequest(
        String city,
        LocalDate checkIn,
        LocalDate checkOut,
        int travelers,
        int rooms
) {
}