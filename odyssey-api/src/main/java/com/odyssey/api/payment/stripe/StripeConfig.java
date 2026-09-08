package com.odyssey.api.payment.stripe;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the Stripe Java SDK's global API key from {@link StripeProperties}
 * (itself backed by the STRIPE_SECRET_KEY environment variable). The
 * secret key never reaches the React frontend: it is only ever read here,
 * server-side.
 */
@Configuration
@EnableConfigurationProperties(StripeProperties.class)
public class StripeConfig {

    private final StripeProperties properties;

    public StripeConfig(StripeProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void configureStripeApiKey() {
        Stripe.apiKey = properties.getSecretKey();
    }
}
