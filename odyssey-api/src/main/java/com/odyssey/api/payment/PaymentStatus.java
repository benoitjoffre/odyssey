package com.odyssey.api.payment;

/**
 * Lifecycle of a {@link Payment}.
 *
 * <p>Kept intentionally minimal for this iteration. A {@code REFUNDED}
 * value will be introduced later once refunds are implemented; it is not
 * added yet so we don't ship half-built behavior.</p>
 */
public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED
}
