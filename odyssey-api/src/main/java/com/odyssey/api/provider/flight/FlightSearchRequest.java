package com.odyssey.api.provider.flight;

import java.time.LocalDate;

public record FlightSearchRequest(
        String origin,
        String destination,
        LocalDate departureDate,
        LocalDate returnDate,
        int travelers
) {
}