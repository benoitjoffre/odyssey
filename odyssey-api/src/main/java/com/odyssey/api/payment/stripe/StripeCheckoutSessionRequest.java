package com.odyssey.api.payment.stripe;

/**
 * Everything {@link StripeGateway#createCheckoutSession} needs to create a
 * Stripe-hosted Checkout Session. Amounts are always computed
 * server-side by the caller (never trust a frontend-supplied amount).
 */
public record StripeCheckoutSessionRequest(
    long amountInSmallestCurrencyUnit,
    String currency,
    String description,
    String clientReferenceId,
    String paymentIdMetadata,
    String successUrl,
    String cancelUrl
) {
}
