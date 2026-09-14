package com.odyssey.api.booking;

import java.math.BigDecimal;

public record TripSummaryResponse(
    Long id,
    String title,
    String startDate,
    String endDate,
    BigDecimal assistanceFee

) {}