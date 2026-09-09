package com.odyssey.api.payment;

import com.odyssey.api.quote.Quote;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A Payment belongs to exactly one accepted {@link Quote}. It represents
 * money paid by the Traveler to <strong>Odyssey</strong> only.
 *
 * <p>Odyssey is an assistance service: it never resells the travel
 * service and never collects the Provider's money. The Traveler pays
 * the Provider ({@code providerAmount}) directly, outside of Odyssey/
 * Stripe. The only amount Odyssey ever charges via Stripe, and the only
 * amount this Payment's lifecycle (PENDING/PAID/FAILED) actually tracks,
 * is {@link #assistanceFee}.</p>
 *
 * <p>{@code providerAmount} and {@code totalAmount} are kept here purely
 * as an informational snapshot of the Quote's breakdown at the time the
 * Payment was created (useful for display/audit, and for a future
 * Stripe Connect split). They are never sent to Stripe and never used
 * to decide whether the Payment is PAID: only {@link #assistanceFee} is
 * authoritative for that.</p>
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Odyssey's invariant is ONE Payment per Quote: a Quote is paid at most
     * once, and every Checkout retry (PENDING re-click, FAILED retry)
     * reuses this same row rather than creating a new one. The
     * {@code unique = true} join column turns this into an actual database
     * constraint (not just an application convention), so that even a bug
     * or a race that slips past the {@code Quote} row lock in
     * {@code PaymentService.createCheckoutSession} can never result in two
     * Payment rows for the same Quote.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "quote_id", nullable = false, unique = true)
    private Quote quote;

    /**
     * Informational only: the Provider's share of the estimated total
     * cost, paid by the Traveler directly to the Provider. Never charged
     * by Odyssey/Stripe.
     */
    @Column(nullable = false)
    private BigDecimal providerAmount;

    /**
     * The ONLY amount Odyssey actually collects from the Traveler via
     * Stripe. This is what is sent to Stripe Checkout and what the
     * verified webhook reconciles against before marking this Payment
     * PAID.
     */
    @Column(nullable = false)
    private BigDecimal assistanceFee;

    /**
     * Informational only: {@code providerAmount + assistanceFee} at the
     * time this Payment was created, i.e. the estimated total cost shown
     * to the Traveler. NOT the amount charged by Stripe.
     */
    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String stripeCheckoutSessionId;

    private String stripePaymentIntentId;

    /**
     * Counts distinct Checkout attempts for this Payment: starts at 1 when
     * the Payment is first created, and is incremented only when a fresh
     * Stripe Checkout Session is genuinely needed (initial attempt, or a
     * retry after a previous attempt ended {@code FAILED}). Re-clicking
     * Pay while still {@code PENDING} does NOT increment this value.
     *
     * <p>Used to build a stable Stripe idempotency key
     * ({@code payment-<id>-attempt-<n>}) in {@code PaymentService}: as long
     * as the attempt number is unchanged, Stripe treats repeated Checkout
     * Session creation calls for this Payment as the exact same request and
     * returns the same cached Session instead of creating a duplicate one,
     * while a new attempt number (after a FAILED retry) always results in a
     * genuinely fresh Session.</p>
     *
     * <p>Not marked {@code nullable = false} so this column can be added to
     * the existing, already-populated {@code payments} table via
     * {@code ddl-auto=update} without failing; application code always sets
     * it when creating/reusing a Payment.</p>
     */
    private Integer checkoutAttempt;

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

    public Integer getCheckoutAttempt() {
        return checkoutAttempt;
    }

    public void setCheckoutAttempt(Integer checkoutAttempt) {
        this.checkoutAttempt = checkoutAttempt;
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
