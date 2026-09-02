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

    @Column(nullable = false)
    private BigDecimal sellingPrice;

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

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
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