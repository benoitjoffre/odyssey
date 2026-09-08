package com.odyssey.api.payment;

import com.odyssey.api.quote.Quote;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A Payment belongs to exactly one accepted {@link Quote}. It records the
 * authoritative, server-computed amounts (never trusted from the
 * frontend) and the Stripe Checkout/PaymentIntent identifiers needed to
 * reconcile the Stripe-hosted payment lifecycle.
 *
 * <p>Odyssey does not resell the travel service: {@code providerAmount}
 * belongs to the provider and {@code assistanceFee} is Odyssey's own
 * remuneration. Keeping them separate here (rather than only storing the
 * total) is what will allow a clean introduction of Stripe Connect later
 * (splitting the transfer between the connected provider account and
 * Odyssey) without reshaping this table.</p>
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    @Column(nullable = false)
    private BigDecimal providerAmount;

    @Column(nullable = false)
    private BigDecimal assistanceFee;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String stripeCheckoutSessionId;

    private String stripePaymentIntentId;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant paidAt;

    public Payment() {
    }

    public Long getId() {
        return id;
    }

    public Quote getQuote() {
        return quote;
    }

    public void setQuote(Quote quote) {
        this.quote = quote;
    }

    public BigDecimal getProviderAmount() {
        return providerAmount;
    }

    public void setProviderAmount(BigDecimal providerAmount) {
        this.providerAmount = providerAmount;
    }

    public BigDecimal getAssistanceFee() {
        return assistanceFee;
    }

    public void setAssistanceFee(BigDecimal assistanceFee) {
        this.assistanceFee = assistanceFee;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getStripeCheckoutSessionId() {
        return stripeCheckoutSessionId;
    }

    public void setStripeCheckoutSessionId(String stripeCheckoutSessionId) {
        this.stripeCheckoutSessionId = stripeCheckoutSessionId;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }

    public void setStripePaymentIntentId(String stripePaymentIntentId) {
        this.stripePaymentIntentId = stripePaymentIntentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }
}
