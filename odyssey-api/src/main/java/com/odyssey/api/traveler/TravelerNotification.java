package com.odyssey.api.traveler;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "traveler_notifications")
public class TravelerNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "traveler_id", nullable = false)
    private Traveler traveler;

    @jakarta.persistence.Column(nullable = false)
    private String type;

    @jakarta.persistence.Column(nullable = false)
    private String title;

    @jakarta.persistence.Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @jakarta.persistence.Column(nullable = false)
    private String targetUrl;

    @jakarta.persistence.Column(nullable = false)
    private boolean read;

    @jakarta.persistence.Column(nullable = false)
    private Instant createdAt;

    public TravelerNotification() {}

    public Long getId() {
        return id;
    }

    public Traveler getTraveler() {
        return traveler;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public boolean isRead() {
        return read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setTraveler(Traveler traveler) {
        this.traveler = traveler;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
