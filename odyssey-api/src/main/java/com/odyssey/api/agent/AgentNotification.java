package com.odyssey.api.agent;

import com.odyssey.api.booking.BookingRequest;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "agent_notifications")
public class AgentNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @ManyToOne
    @JoinColumn(name = "booking_request_id", nullable = false)
    private BookingRequest bookingRequest;

    private String message;

    private boolean read;

    private Instant createdAt;

    public AgentNotification() {}

    public Long getId() {
        return id;
    }

    public Agent getAgent() {
        return agent;
    }

    public BookingRequest getBookingRequest() {
        return bookingRequest;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public void setBookingRequest(BookingRequest bookingRequest) {
        this.bookingRequest = bookingRequest;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}