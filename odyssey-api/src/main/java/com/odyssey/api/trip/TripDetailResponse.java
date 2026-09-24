package com.odyssey.api.trip;

import com.odyssey.api.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TripDetailResponse(
    Long id,
    String title,
    LocalDate startDate,
    LocalDate endDate,
    TripStatus status,
    Long travelerId,
    List<TripNeedResponse> needs,
    Long travelEventId,
    BigDecimal assistanceFee,
    boolean assistanceFeePayable,
    PaymentStatus paymentStatus
) {}