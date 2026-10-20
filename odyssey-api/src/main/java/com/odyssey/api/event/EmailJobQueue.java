package com.odyssey.api.event;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class EmailJobQueue {

    private final EmailJobRepository repository;
    private final EmailNotificationPlanner planner;
    private final ObjectMapper mapper;

    public EmailJobQueue(EmailJobRepository repository, EmailNotificationPlanner planner, ObjectMapper mapper) {
        this.repository = repository;
        this.planner = planner;
        this.mapper = mapper;
    }

    @Transactional
    public void enqueue(Long outboxId, String eventType, String payload) {
        List<String> audiences = switch (eventType) {
            case "BOOKING_REQUESTED", "PAYMENT_SUCCEEDED" -> List.of("TRAVELER", "AGENT");
            case "BOOKING_ASSIGNED", "QUOTE_SENT", "TRIP_QUOTES_SENT", "TRAVELER_ONBOARDING_COMPLETED",
                 "BOOKING_CONFIRMED", "PROVIDER_PAYMENT_REQUIRED" -> List.of("TRAVELER");
            case "QUOTE_ACCEPTED", "QUOTE_REJECTED" -> List.of("AGENT");
            default -> List.of();
        };
        for (String audience : audiences) {
            String key = "outbox-" + outboxId + "-" + audience;
            if (!repository.existsByDeduplicationKey(key)) {
                repository.save(new EmailJob(key, audience + ":" + eventType, payload));
            }
        }
    }

    @Transactional
    public void prepare(EmailJob job) {
        List<EmailMessage> messages = planner.prepare(job.getType(), job.getPayload());
        for (int i = 0; i < messages.size(); i++) {
            EmailMessage message = messages.get(i);
            if (message.to() == null || message.to().isBlank()) {
                throw new IllegalArgumentException("Email recipient must not be blank");
            }
            String key = job.getDeduplicationKey() + "-delivery-" + i;
            if (!repository.existsByDeduplicationKey(key)) {
                repository.save(new EmailJob(key, "DELIVERY", mapper.writeValueAsString(message)));
            }
        }
        // Persist every recipient and complete preparation atomically, before any network call.
        repository.flush();
        if (repository.complete(job.getId(), job.getLeaseToken(), Instant.now(), null) != 1) {
            throw new IllegalStateException("Email preparation lease lost for job " + job.getId());
        }
    }
}
