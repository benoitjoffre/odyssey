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

    private String providerConfirmationId;

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
}