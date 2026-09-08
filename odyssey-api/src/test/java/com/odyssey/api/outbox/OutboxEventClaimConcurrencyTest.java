package com.odyssey.api.outbox;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the atomic compare-and-swap claim used by {@link OutboxProcessor}
 * at the database level: once an event has been claimed (moved out of
 * PENDING), a second concurrent claim attempt on the same row must fail
 * (0 rows updated), preventing two processing runs from handling the same
 * event twice.
 */
@SpringBootTest
@Transactional
class OutboxEventClaimConcurrencyTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void secondClaimAttemptOnSameEventIsRejected() {

        OutboxEvent event = new OutboxEvent();
        event.setEventType("BOOKING_REQUESTED");
        event.setPayload("{}");
        event.setStatus(OutboxStatus.PENDING);
        event.setCreatedAt(Instant.now());
        event = outboxEventRepository.save(event);

        int firstClaim = outboxEventRepository.updateStatusIfCurrent(
            event.getId(), OutboxStatus.PENDING, OutboxStatus.PROCESSING
        );

        int secondClaim = outboxEventRepository.updateStatusIfCurrent(
            event.getId(), OutboxStatus.PENDING, OutboxStatus.PROCESSING
        );

        assertEquals(1, firstClaim);
        assertEquals(0, secondClaim);

        OutboxEvent reloaded = outboxEventRepository
            .findById(event.getId())
            .orElseThrow();

        assertEquals(OutboxStatus.PROCESSING, reloaded.getStatus());
    }
}
