package com.odyssey.api.quote;

import com.odyssey.api.booking.BookingRequest;
import com.odyssey.api.booking.confirmation.Booking;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "quotes")
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "booking_request_id")
    private BookingRequest bookingRequest;

    @OneToOne(mappedBy = "quote", cascade = CascadeType.REMOVE)
    private Booking booking;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String externalOfferId;

    @Column(nullable = false)
    private BigDecimal providerPrice;

    /**
     * Odyssey's assistance fee for helping the traveler with this
     * reservation. Odyssey does not resell the travel service: the
     * provider amount belongs to the provider, this fee is Odyssey's own
     * remuneration. Supplied by the agent, never trusted from the
     * traveler-facing frontend.
     *
     * <p>Not marked {@code nullable = false} at the JPA level so that this
     * column can be added to an existing table via
     * {@code ddl-auto=update} without failing on pre-existing rows;
     * non-nullity for newly created quotes is enforced in
     * {@link QuoteService}.</p>
     */
    private BigDecimal assistanceFee;

    /**
     * Estimated TOTAL cost of the trip for the traveler, ALWAYS computed
     * server-side as {@code providerPrice + assistanceFee}, never supplied
     * by the frontend. Mapped to the pre-existing {@code selling_price}
     * column (renamed at the Java level only, to avoid an orphaned NOT
     * NULL column on this incrementally-migrated schema).
     *
     * <p><strong>This is NOT the amount collected by Odyssey.</strong>
     * Odyssey is an assistance service: it never resells the travel
     * service, so it never collects {@code providerPrice} on the
     * provider's behalf. The Traveler pays {@code providerPrice} directly
     * to the Provider (outside of Odyssey/Stripe) and pays only
     * {@code assistanceFee} to Odyssey via Stripe. This field exists
     * purely so the Traveler can see the overall estimated cost of their
     * trip; see {@link Payment} for what is actually charged.</p>
     */
    @Column(name = "selling_price", nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private String currency;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuoteStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant expiresAt;

    public Quote() {
    }

    public Long getId() {
        return id;
    }

    public BookingRequest getBookingRequest() {
        return bookingRequest;
    }

    public void setBookingRequest(BookingRequest bookingRequest) {
        this.bookingRequest = bookingRequest;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getExternalOfferId() {
        return externalOfferId;
    }

    public void setExternalOfferId(String externalOfferId) {
        this.externalOfferId = externalOfferId;
    }

    public BigDecimal getProviderPrice() {
        return providerPrice;
    }

    public void setProviderPrice(BigDecimal providerPrice) {
        this.providerPrice = providerPrice;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public QuoteStatus getStatus() {
        return status;
    }

    public void setStatus(QuoteStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}