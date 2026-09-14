package com.odyssey.api.trip;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TripResponse(
    Long id,
    String title,
    LocalDate startDate,
    LocalDate endDate,
    TripStatus status,
    Long travelerId,
    Long travelEventId,
    BigDecimal assistanceFee
) {
}
