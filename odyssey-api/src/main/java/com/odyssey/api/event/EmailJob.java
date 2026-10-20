package com.odyssey.api.event;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "email_jobs", indexes = @Index(name = "email_jobs_pending", columnList = "status,nextAttemptAt"))
public class EmailJob {

    public enum Status { PENDING, PROCESSING, COMPLETED, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String deduplicationKey;
    @Column(nullable = false)
    private String type;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
    private int attempts;
    private Instant createdAt;
    private Instant nextAttemptAt;
    private Instant startedAt;
    private Instant firstAttemptAt;
    private Instant completedAt;
    private String leaseToken;
    private String providerId;
    @Column(columnDefinition = "TEXT")
    private String lastError;

    protected EmailJob() {}

    public EmailJob(String key, String type, String payload) {
        this.deduplicationKey = key;
        this.type = type;
        this.payload = payload;
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
        this.nextAttemptAt = createdAt;
    }

    public Long getId() { return id; }
    public String getDeduplicationKey() { return deduplicationKey; }
    public String getType() { return type; }
    public String getPayload() { return payload; }
    public Status getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public Instant getFirstAttemptAt() { return firstAttemptAt; }
    public String getLeaseToken() { return leaseToken; }
    public String getProviderId() { return providerId; }
    public String getLastError() { return lastError; }
}
