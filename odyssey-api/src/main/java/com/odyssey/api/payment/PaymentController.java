package com.odyssey.api.payment;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PreAuthorize("hasRole('TRAVELER')")
    @PostMapping("/api/trips/{tripId}/payment/checkout")
    public CheckoutSessionResponse createCheckoutSession(
        @PathVariable Long tripId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        System.out.println("=== CHECKOUT HIT ===");
        System.out.println("tripId = " + tripId);
        System.out.println("subject = " + jwt.getSubject());
        try {
            return paymentService.createCheckoutSession(tripId, jwt.getSubject());

        } catch (IllegalArgumentException e) {
            System.err.println("=== CHECKOUT ERROR ===");
            System.err.println("tripId = " + tripId);
            System.err.println("subject = " + jwt.getSubject());
            System.err.println("error = " + e.getMessage());
            throw e;
        }
    }

    /**
     * Stripe webhook endpoint. The raw request body is required (not a
     * parsed DTO) so the signature in {@code Stripe-Signature} can be
     * verified against the exact bytes Stripe sent.
     */
    @PostMapping("/api/payments/webhook")
    public ResponseEntity<Void> handleWebhook(
        @RequestBody String payload,
        @RequestHeader("Stripe-Signature") String signature
    ) {
        paymentService.handleWebhookPayload(payload, signature);
        return ResponseEntity.ok().build();
    }
}
