package com.odyssey.api.event;

import java.math.BigDecimal;

/**
 * Published (via the transactional Outbox, like the other domain events
 * in this codebase) once a Stripe webhook has verified that a Payment
 * succeeded. Triggers the agent notification that the supplier Booking
 * can now be created/confirmed.
 */
public record PaymentSucceededEvent(
    Long paymentId,
    Long quoteId,
    Long bookingRequestId,
    Long travelerId,
    Long agentId,
    BigDecimal totalAmount,
    String currency
) {
}
