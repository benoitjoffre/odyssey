package com.odyssey.api.payment;

/**
 * Response returned to the Traveler frontend when a Stripe Checkout
 * Session has been created for a Quote. Intentionally minimal: the
 * frontend only needs the URL to redirect to Stripe-hosted Checkout.
 */
public record CheckoutSessionResponse(
    Long paymentId,
    String checkoutUrl
) {
}
