package com.odyssey.api.booking.confirmation;

import com.odyssey.api.quote.Quote;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(
        name = "quote_id",
        nullable = false,
        unique = true
    )
    private Quote quote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant confirmedAt;

    /**
     * Odyssey-internal confirmation id, generated when the Agent
     * confirms the booking (see {@link FakeBookingProvider}). Distinct
     * from {@link #providerReference}, which the Agent enters manually
     * from information obtained directly from the Provider.
     */
    private String providerConfirmationId;

    /**
     * Reference/confirmation number communicated by the Provider to the
     * Agent (e.g. over phone/email), entered manually. Required before
     * the booking can be confirmed.
     */
    private String providerReference;

    /**
     * External, Provider-controlled payment page the Traveler must use
     * to pay the Provider directly. Odyssey never proxies this payment
     * and never stores the Traveler's payment credentials.
     */
    private String providerPaymentUrl;

    /**
     * What the Agent knows about the Traveler's direct payment to the
     * Provider. Entirely independent from the Odyssey assistance
     * {@code Payment}: it is never set automatically from it.
     *
     * <p>Nullable at the DB level (unlike most other columns here) on
     * purpose: this column was added to an already-populated table under
     * {@code ddl-auto=update}, which cannot add a NOT NULL column without
     * a default to a non-empty table. Application code always sets a
     * value (defaulting to {@code NOT_REQUIRED_YET}) on every new/updated
     * Booking; only rows persisted before this change may read back as
     * {@code null}.</p>
     */
    @Enumerated(EnumType.STRING)
    private ProviderPaymentStatus providerPaymentStatus;

    public Booking() {
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

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public String getProviderConfirmationId() {
        return providerConfirmationId;
    }

    public void setProviderConfirmationId(
        String providerConfirmationId
    ) {
        this.providerConfirmationId = providerConfirmationId;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public void setProviderReference(String providerReference) {
        this.providerReference = providerReference;
    }

    public String getProviderPaymentUrl() {
        return providerPaymentUrl;
    }

    public void setProviderPaymentUrl(String providerPaymentUrl) {
        this.providerPaymentUrl = providerPaymentUrl;
    }

    public ProviderPaymentStatus getProviderPaymentStatus() {
        return providerPaymentStatus;
    }

    public void setProviderPaymentStatus(ProviderPaymentStatus providerPaymentStatus) {
        this.providerPaymentStatus = providerPaymentStatus;
    }
}