package com.odyssey.api.event;

import java.math.BigDecimal;

/**
 * Published via the transactional Outbox once Stripe has verified
 * that the Odyssey assistance fee for a Trip has been paid.
 */

public record PaymentSucceededEvent(
    Long paymentId,
    Long tripId,
    Long travelerId,
    BigDecimal assistanceFee,
    String currency
) {
}
