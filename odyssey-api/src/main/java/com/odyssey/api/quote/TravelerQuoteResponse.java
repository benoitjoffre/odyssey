package com.odyssey.api.quote;

import com.odyssey.api.booking.confirmation.ProviderPaymentStatus;
import com.odyssey.api.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record TravelerQuoteResponse(
    Long id,
    Long bookingRequestId,
    BigDecimal providerPrice,
    BigDecimal assistanceFee,
    BigDecimal totalAmount,
    String currency,
    String description,
    QuoteStatus status,
    Instant createdAt,
    Instant expiresAt,
    PaymentStatus paymentStatus,
    String providerPaymentUrl,
    ProviderPaymentStatus providerPaymentStatus
) {}