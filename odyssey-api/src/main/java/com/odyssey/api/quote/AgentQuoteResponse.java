package com.odyssey.api.quote;

import com.odyssey.api.booking.confirmation.ProviderPaymentStatus;
import com.odyssey.api.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record AgentQuoteResponse(
    Long id,
    Long bookingRequestId,
    String provider,
    String externalOfferId,
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