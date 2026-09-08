package com.odyssey.api.booking;

import com.odyssey.api.agent.Agent;
import com.odyssey.api.agent.AgentNotification;
import com.odyssey.api.agent.AgentNotificationRepository;
import com.odyssey.api.agent.AgentRepository;
import com.odyssey.api.agent.AgentStatus;
import com.odyssey.api.need.Need;
import com.odyssey.api.need.NeedRepository;
import com.odyssey.api.need.NeedStatus;
import com.odyssey.api.need.NeedType;
import com.odyssey.api.outbox.OutboxEvent;
import com.odyssey.api.outbox.OutboxEventRepository;
import com.odyssey.api.outbox.OutboxProcessor;
import com.odyssey.api.outbox.OutboxStatus;
import com.odyssey.api.quote.CreateQuoteRequest;
import com.odyssey.api.quote.QuoteResponse;
import com.odyssey.api.quote.QuoteService;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerRepository;
import com.odyssey.api.trip.Trip;
import com.odyssey.api.trip.TripRepository;
import com.odyssey.api.trip.TripStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end test of the Outbox → OutboxProcessor → Spring event → listener
 * flow that replaced the previous Kafka-based transport, proving the exact
 * existing business reactions (agent notifications) still work.
 */
@SpringBootTest
@Transactional
class BookingRequestOutboxFlowIntegrationTest {

    @Autowired private TravelerRepository travelerRepository;
    @Autowired private TripRepository tripRepository;
    @Autowired private NeedRepository needRepository;
    @Autowired private AgentRepository agentRepository;
    @Autowired private BookingRequestService bookingRequestService;
    @Autowired private QuoteService quoteService;
    @Autowired private OutboxEventRepository outboxEventRepository;
    @Autowired private OutboxProcessor outboxProcessor;
    @Autowired private AgentNotificationRepository agentNotificationRepository;

    @Test
    void bookingRequestedFlowsThroughOutboxToAgentNotification() {

        Need need = createNeed();
        Agent agent = createAvailableAgent();

        BookingRequestResponse bookingRequest = bookingRequestService
            .createBookingRequest(new CreateBookingRequest(need.getId(), "notes"));

        List<OutboxEvent> pending =
            outboxEventRepository.findByStatus(OutboxStatus.PENDING);
        assertEquals(1, pending.size());
        assertEquals("BOOKING_REQUESTED", pending.get(0).getEventType());

        outboxProcessor.processPendingEvents();

        OutboxEvent processed = outboxEventRepository
            .findById(pending.get(0).getId())
            .orElseThrow();
        assertEquals(OutboxStatus.PROCESSED, processed.getStatus());
        assertTrue(
            outboxEventRepository.findByStatus(OutboxStatus.PENDING).isEmpty()
        );

        List<AgentNotification> notifications = agentNotificationRepository
            .findByAgentIdOrderByCreatedAtDesc(agent.getId());

        assertEquals(1, notifications.size());
        assertEquals(
            bookingRequest.id(),
            notifications.get(0).getBookingRequest().getId()
        );
    }

    @Test
    void quoteAcceptedFlowsThroughOutboxToAgentNotification() {

        Need need = createNeed();
        Agent agent = createAvailableAgent();

        BookingRequestResponse bookingRequest = bookingRequestService
            .createBookingRequest(new CreateBookingRequest(need.getId(), "notes"));
        outboxProcessor.processPendingEvents();

        bookingRequestService.claimBookingRequest(bookingRequest.id(), agent.getId());
        outboxProcessor.processPendingEvents();

        QuoteResponse quote = quoteService.createQuote(
            bookingRequest.id(),
            agent.getId(),
            new CreateQuoteRequest(
                "provider",
                "offer-1",
                BigDecimal.TEN,
                BigDecimal.valueOf(20),
                "EUR",
                "description",
                null
            )
        );

        quoteService.sendQuote(quote.id(), agent.getId());
        outboxProcessor.processPendingEvents();

        quoteService.acceptQuote(quote.id(), need.getTrip().getTraveler().getId());
        outboxProcessor.processPendingEvents();

        assertTrue(
            outboxEventRepository.findByStatus(OutboxStatus.PENDING).isEmpty()
        );

        List<AgentNotification> notifications = agentNotificationRepository
            .findByAgentIdOrderByCreatedAtDesc(agent.getId());

        assertTrue(
            notifications.stream().anyMatch(
                notification -> notification.getMessage()
                    .contains("a accepté votre proposition")
            )
        );
    }

    private Need createNeed() {

        Traveler traveler = travelerRepository.save(new Traveler(
            "Outbox",
            "Flow",
            "outbox-flow-" + UUID.randomUUID() + "@example.com"
        ));

        Trip trip = new Trip();
        trip.setTitle("Outbox flow trip");
        trip.setStartDate(LocalDate.now().plusDays(1));
        trip.setEndDate(LocalDate.now().plusDays(2));
        trip.setStatus(TripStatus.DRAFT);
        trip.setTraveler(traveler);
        trip = tripRepository.save(trip);

        Need need = new Need();
        need.setType(NeedType.CAR);
        need.setStatus(NeedStatus.DRAFT);
        need.setTrip(trip);

        return needRepository.save(need);
    }

    private Agent createAvailableAgent() {

        // Neutralize any other AVAILABLE agents already present in the
        // database (e.g. leftover from manual testing) so that the
        // "first available agent" lookup deterministically picks the
        // agent created by this test.
        agentRepository.findByStatus(AgentStatus.AVAILABLE)
            .forEach(existing -> {
                existing.setStatus(AgentStatus.BUSY);
                agentRepository.save(existing);
            });

        Agent agent = new Agent();
        agent.setFirstName("Available");
        agent.setLastName("Agent");
        agent.setEmail("outbox-agent-" + UUID.randomUUID() + "@example.com");
        agent.setStatus(AgentStatus.AVAILABLE);

        return agentRepository.save(agent);
    }
}
