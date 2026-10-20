package com.odyssey.api.event;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import com.odyssey.api.booking.BookingRequestRepository;
import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.trip.TripRepository;

@Service
public class EmailJobQueue {

    private final EmailJobRepository repository;
    private final EmailNotificationPlanner planner;
    private final ObjectMapper mapper;
    private final BookingRequestRepository bookingRequestRepository;
    private final TripRepository tripRepository;

    public EmailJobQueue(EmailJobRepository repository, EmailNotificationPlanner planner, ObjectMapper mapper,
        BookingRequestRepository bookingRequestRepository, TripRepository tripRepository) {
        this.repository = repository;
        this.planner = planner;
        this.mapper = mapper;
        this.bookingRequestRepository = bookingRequestRepository;
        this.tripRepository = tripRepository;
    }

    @Transactional
    public void enqueue(Long outboxId, String eventType, String payload) {
        List<String> audiences = switch (eventType) {
            case "BOOKING_REQUESTED", "PAYMENT_SUCCEEDED" -> List.of("TRAVELER", "AGENT");
            case "BOOKING_ASSIGNED", "QUOTE_SENT", "TRIP_QUOTES_SENT", "TRAVELER_ONBOARDING_COMPLETED",
                 "TRIP_BOOKINGS_CONFIRMED", "PROVIDER_PAYMENT_REQUIRED" -> List.of("TRAVELER");
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
        String deliveryKey = job.getDeduplicationKey() + "-delivery-";
        if ("TRAVELER:BOOKING_ASSIGNED".equals(job.getType())) {
            BookingAssignedEvent event = mapper.readValue(job.getPayload(), BookingAssignedEvent.class);
            Long tripId = bookingRequestRepository.findById(event.bookingRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking request not found"))
                .getNeed().getTrip().getId();
            // Serialize preparation for this trip so concurrent assignments share one durable delivery.
            tripRepository.findByIdForUpdate(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
            deliveryKey = "trip-" + tripId + "-traveler-assigned-delivery-";
        }
        List<EmailMessage> messages = repository.existsByDeduplicationKey(deliveryKey + "0")
            ? List.of() : planner.prepare(job.getType(), job.getPayload());
        for (int i = 0; i < messages.size(); i++) {
            EmailMessage message = messages.get(i);
            if (message.to() == null || message.to().isBlank()) {
                throw new IllegalArgumentException("Email recipient must not be blank");
            }
            String key = deliveryKey + i;
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
