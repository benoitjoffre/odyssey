package com.odyssey.api.outbox;

import com.odyssey.api.event.BookingAssignedEvent;
import com.odyssey.api.event.BookingRequestedEvent;
import com.odyssey.api.event.PaymentSucceededEvent;
import com.odyssey.api.event.QuoteAcceptedEvent;
import com.odyssey.api.event.QuoteSentEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Periodically processes pending {@link OutboxEvent}s persisted by business
 * transactions and republishes them in-process as Spring application events.
 *
 * <p>The PostgreSQL outbox remains the sole durability/atomicity mechanism
 * (a business transaction and its {@link OutboxEvent} row are committed
 * together). This processor replaces the previous Kafka-based publishing
 * stage: instead of sending to a broker, it dispatches the deserialized
 * domain event via {@link ApplicationEventPublisher}, which invokes any
 * matching {@code @EventListener} beans synchronously, in-process.</p>
 *
 * <p>Each event is claimed with an atomic conditional update
 * (PENDING → PROCESSING) so that two overlapping processing runs can never
 * both process the same event. On success the event is marked PROCESSED;
 * on failure it is reverted to PENDING so a later run retries it, and the
 * failure is logged (the event is never silently discarded).</p>
 */
@Service
public class OutboxProcessor {

    private static final Logger logger =
        LoggerFactory.getLogger(OutboxProcessor.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Map<String, Function<String, Object>> eventReaders;

    public OutboxProcessor(
        OutboxEventRepository outboxEventRepository,
        ApplicationEventPublisher applicationEventPublisher,
        ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.applicationEventPublisher = applicationEventPublisher;

        this.eventReaders = Map.of(
            "BOOKING_REQUESTED", payload ->
                objectMapper.readValue(payload, BookingRequestedEvent.class),
            "BOOKING_ASSIGNED", payload ->
                objectMapper.readValue(payload, BookingAssignedEvent.class),
            "QUOTE_SENT", payload ->
                objectMapper.readValue(payload, QuoteSentEvent.class),
            "QUOTE_ACCEPTED", payload ->
                objectMapper.readValue(payload, QuoteAcceptedEvent.class),
            "PAYMENT_SUCCEEDED", payload ->
                objectMapper.readValue(payload, PaymentSucceededEvent.class)
        );
    }

    @Scheduled(fixedDelay = 5000)
    public void processPendingEvents() {

        List<OutboxEvent> pendingEvents =
            outboxEventRepository.findByStatus(OutboxStatus.PENDING);

        for (OutboxEvent pendingEvent : pendingEvents) {
            processEvent(pendingEvent.getId());
        }
    }

    private void processEvent(Long eventId) {

        boolean claimed = outboxEventRepository.updateStatusIfCurrent(
            eventId,
            OutboxStatus.PENDING,
            OutboxStatus.PROCESSING
        ) == 1;

        if (!claimed) {
            // Already claimed (or processed) by another execution.
            return;
        }

        try {
            OutboxEvent event = outboxEventRepository
                .findById(eventId)
                .orElseThrow();

            Object domainEvent = deserialize(
                event.getEventType(),
                event.getPayload()
            );

            applicationEventPublisher.publishEvent(domainEvent);

            outboxEventRepository.updateStatusIfCurrent(
                eventId,
                OutboxStatus.PROCESSING,
                OutboxStatus.PROCESSED
            );

        } catch (Exception exception) {

            logger.error(
                "Failed to process outbox event {}; it will be retried",
                eventId,
                exception
            );

            outboxEventRepository.updateStatusIfCurrent(
                eventId,
                OutboxStatus.PROCESSING,
                OutboxStatus.PENDING
            );
        }
    }

    private Object deserialize(String eventType, String payload) {

        Function<String, Object> reader = eventReaders.get(eventType);

        if (reader == null) {
            throw new IllegalStateException(
                "Unknown outbox event type: " + eventType
            );
        }

        return reader.apply(payload);
    }
}
