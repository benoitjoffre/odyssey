package com.odyssey.api.payment.stripe;

/**
 * Everything {@link StripeGateway#createCheckoutSession} needs to create a
 * Stripe-hosted Checkout Session. Amounts are always computed
 * server-side by the caller (never trust a frontend-supplied amount).
 *
 * <p>{@code idempotencyKey} lets {@code PaymentService} ask Stripe to treat
 * repeated calls for the same Payment attempt (e.g. a duplicate Checkout
 * click, or an application retry) as the exact same request: Stripe
 * returns the previously created Session instead of creating a duplicate
 * one, as long as the key and request payload are unchanged.</p>
 */
public record StripeCheckoutSessionRequest(
    long amountInSmallestCurrencyUnit,
    String currency,
    String description,
    String clientReferenceId,
    String paymentIdMetadata,
    String successUrl,
    String cancelUrl,
    String idempotencyKey
) {
}
