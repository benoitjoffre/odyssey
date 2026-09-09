package com.odyssey.api.payment.stripe;

/**
 * A verified, parsed Stripe webhook event, reduced to what
 * {@code PaymentService} needs. Only populated for the event types we
 * handle; {@code checkoutSessionId}/{@code paymentIntentId}/
 * {@code amountTotal}/{@code currency} may be {@code null} for other
 * event types.
 *
 * <p>{@code amountTotal} (smallest currency unit, e.g. cents) and
 * {@code currency} are what Stripe actually reports for this Checkout
 * Session; {@code PaymentService} reconciles them against the persisted
 * Payment's assistance fee before ever marking it PAID.</p>
 *
 * <p>Handled event types, see {@code PaymentService.processVerifiedEvent}:
 * <ul>
 *   <li>{@link #CHECKOUT_SESSION_COMPLETED} — the only success signal.</li>
 *   <li>{@link #CHECKOUT_SESSION_EXPIRED} — Checkout Session abandoned
 *       (default 24h expiry with no completed payment).</li>
 *   <li>{@link #CHECKOUT_SESSION_ASYNC_PAYMENT_FAILED} — a delayed
 *       (asynchronous) payment method attached to this Session ultimately
 *       failed. Odyssey's Checkout Sessions don't pin
 *       {@code payment_method_types}, so the set of enabled payment
 *       methods is controlled by the Stripe Dashboard, not by this
 *       codebase; this event is handled defensively in case an
 *       asynchronous method (e.g. SEPA Direct Debit) is ever enabled
 *       there.</li>
 * </ul>
 * {@code payment_intent.payment_failed} is intentionally NOT handled:
 * {@code Payment.stripePaymentIntentId} is only persisted once a payment
 * succeeds (see {@code PaymentRepository.markAsPaidIfNotAlreadyPaid}), so
 * for a still-PENDING Payment there is nothing reliable to correlate this
 * event against.</p>
 */
public record StripeWebhookEvent(
    String eventId,
    String type,
    String checkoutSessionId,
    String paymentIntentId,
    Long amountTotal,
    String currency
) {
    public static final String CHECKOUT_SESSION_COMPLETED = "checkout.session.completed";
    public static final String CHECKOUT_SESSION_EXPIRED = "checkout.session.expired";
    public static final String CHECKOUT_SESSION_ASYNC_PAYMENT_FAILED = "checkout.session.async_payment_failed";

    public boolean isCheckoutSessionCompleted() {
        return CHECKOUT_SESSION_COMPLETED.equals(type);
    }

    public boolean isCheckoutSessionExpired() {
        return CHECKOUT_SESSION_EXPIRED.equals(type);
    }

    public boolean isCheckoutSessionAsyncPaymentFailed() {
        return CHECKOUT_SESSION_ASYNC_PAYMENT_FAILED.equals(type);
    }

    /**
     * Any terminal-failure style event that should transition a PENDING
     * Payment to FAILED (never PAID -> FAILED, see PaymentService).
     */
    public boolean isTerminalFailure() {
        return isCheckoutSessionExpired() || isCheckoutSessionAsyncPaymentFailed();
    }
}
