package com.odyssey.api.payment.stripe;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;

import org.springframework.stereotype.Component;

/**
 * Real Stripe SDK-backed {@link StripeGateway}. Runs against Stripe TEST
 * MODE (the secret key configured via STRIPE_SECRET_KEY determines
 * test/live mode; this codebase only ever expects a test-mode key).
 */
@Component
public class StripeGatewayImpl implements StripeGateway {

    private final StripeProperties properties;

    public StripeGatewayImpl(StripeProperties properties) {
        this.properties = properties;
    }

    @Override
    public StripeCheckoutSession createCheckoutSession(StripeCheckoutSessionRequest request) {
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(request.successUrl())
                .setCancelUrl(request.cancelUrl())
                .setClientReferenceId(request.clientReferenceId())
                .putMetadata("paymentId", request.paymentIdMetadata())
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(
                            SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(request.currency())
                                .setUnitAmount(request.amountInSmallestCurrencyUnit())
                                .setProductData(
                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(request.description())
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build();

            Session session = Session.create(params);

            return new StripeCheckoutSession(session.getId(), session.getUrl());
        } catch (StripeException exception) {
            throw new IllegalStateException(
                "Failed to create Stripe checkout session",
                exception
            );
        }
    }

    @Override
    public StripeWebhookEvent verifyAndParseEvent(String payload, String signatureHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(
                payload,
                signatureHeader,
                properties.getWebhookSecret()
            );
        } catch (SignatureVerificationException exception) {
            throw new IllegalArgumentException(
                "Invalid Stripe webhook signature",
                exception
            );
        }

        String checkoutSessionId = null;
        String paymentIntentId = null;

        if (StripeWebhookEvent.CHECKOUT_SESSION_COMPLETED.equals(event.getType())) {
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            Session session = (Session) deserializer.getObject().orElse(null);
            if (session != null) {
                checkoutSessionId = session.getId();
                paymentIntentId = session.getPaymentIntent();
            }
        }

        return new StripeWebhookEvent(
            event.getId(),
            event.getType(),
            checkoutSessionId,
            paymentIntentId
        );
    }
}
