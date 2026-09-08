package com.odyssey.api.payment.stripe;

/**
 * A verified, parsed Stripe webhook event, reduced to what
 * {@code PaymentService} needs. Only populated for the event types we
 * handle; {@code checkoutSessionId}/{@code paymentIntentId} may be
 * {@code null} for other event types.
 */
public record StripeWebhookEvent(
    String eventId,
    String type,
    String checkoutSessionId,
    String paymentIntentId
) {
    public static final String CHECKOUT_SESSION_COMPLETED = "checkout.session.completed";

    public boolean isCheckoutSessionCompleted() {
        return CHECKOUT_SESSION_COMPLETED.equals(type);
    }
}
