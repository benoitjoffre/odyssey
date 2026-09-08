package com.odyssey.api.payment.stripe;

/** Minimal result of creating a Stripe Checkout Session. */
public record StripeCheckoutSession(
    String id,
    String url
) {
}
