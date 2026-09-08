package com.odyssey.api.payment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/api/quotes/{quoteId}/payment/checkout")
    public CheckoutSessionResponse createCheckoutSession(
        @PathVariable Long quoteId,
        @RequestParam Long travelerId
    ) {
        return paymentService.createCheckoutSession(quoteId, travelerId);
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
