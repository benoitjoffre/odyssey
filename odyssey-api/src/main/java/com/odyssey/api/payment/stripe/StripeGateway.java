package com.odyssey.api.payment.stripe;

/**
 * Abstraction over the Stripe Java SDK so that {@code PaymentService} can
 * be unit-tested without any real Stripe network call (see
 * {@link StripeGatewayImpl} for the real implementation).
 */
public interface StripeGateway {

    StripeCheckoutSession createCheckoutSession(StripeCheckoutSessionRequest request);

    /**
     * Verifies the Stripe-Signature header against the configured webhook
     * secret and parses the event. Purely local/offline HMAC
     * verification: no network call to Stripe is made.
     *
     * @throws IllegalArgumentException if the signature is invalid.
     */
    StripeWebhookEvent verifyAndParseEvent(String payload, String signatureHeader);
}
