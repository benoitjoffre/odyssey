package com.odyssey.api.payment.stripe;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Stripe TEST MODE configuration. Values are provided exclusively through
 * environment variables (see application.properties placeholders); real
 * Stripe credentials must never be committed.
 */
@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {

    /** STRIPE_SECRET_KEY. Server-side only, never exposed to the frontend. */
    private String secretKey;

    /** STRIPE_WEBHOOK_SECRET, used to verify webhook signatures. */
    private String webhookSecret;

    /** Where Stripe redirects the traveler after a successful payment. */
    private String successUrl;

    /** Where Stripe redirects the traveler if they cancel the payment. */
    private String cancelUrl;

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public String getSuccessUrl() {
        return successUrl;
    }

    public void setSuccessUrl(String successUrl) {
        this.successUrl = successUrl;
    }

    public String getCancelUrl() {
        return cancelUrl;
    }

    public void setCancelUrl(String cancelUrl) {
        this.cancelUrl = cancelUrl;
    }
}
