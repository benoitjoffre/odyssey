package com.odyssey.api.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import com.odyssey.api.agent.*;
import com.odyssey.api.booking.*;
import com.odyssey.api.booking.confirmation.*;
import com.odyssey.api.quote.*;
import com.odyssey.api.need.*;
import com.odyssey.api.outbox.*;
import com.odyssey.api.traveler.*;
import com.odyssey.api.trip.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "app.email.enabled=false")
@Transactional
class EmailDeliveryFlowIntegrationTest {

    @Autowired private EmailJobRepository jobs;
    @Autowired private EmailJobQueue queue;
    @Autowired private OutboxProcessor outbox;
    @Autowired private OutboxEventRepository events;
    @Autowired private TravelerRepository travelers;
    @Autowired private TravelerService travelerService;
    @Autowired private TripRepository trips;
    @Autowired private NeedRepository needs;
    @Autowired private AgentRepository agents;
    @Autowired private BookingRequestService requests;
    @Autowired private BookingRequestRepository requestRepository;
    @Autowired private ObjectMapper mapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BookingService bookingService;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private jakarta.persistence.EntityManager entityManager;
    @MockitoBean private EmailService sender;
    @MockitoBean private JwtDecoder jwtDecoder;
    private EmailJobProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new EmailJobProcessor(jobs, queue, sender, mapper, true);
        when(sender.sendEmail(any(), anyString())).thenReturn("resend-test-id");
    }

    @Test
    void onboardingPersistsMessageBeforeSendingAndDoesNotDuplicateOnReplay() {
        Traveler traveler = traveler();
        travelerService.completeOnboarding(traveler.getAuth0Subject(),
            new TravelerOnboardingRequest("Alice", "Martin", "+33600000000", null, "fr"));
        outbox.processPendingEvents();
        assertEquals(1, jobs.count());
        processor.processPending();
        assertEquals(2, jobs.count());
        verifyNoInteractions(sender);

        processor.processPending();
        EmailJob delivery = deliveryJobs().getFirst();
        assertEquals(EmailJob.Status.COMPLETED, delivery.getStatus());
        assertEquals("resend-test-id", delivery.getProviderId());
        EmailMessage message = mapper.readValue(delivery.getPayload(), EmailMessage.class);
        assertEquals(traveler.getEmail(), message.to());
        assertTrue(message.htmlBody().contains("Bienvenue"));

        OutboxEvent event = events.findAll().getFirst();
        events.updateStatusIfCurrent(event.getId(), OutboxStatus.PROCESSED, OutboxStatus.PENDING);
        outbox.processPendingEvents();
        processor.processPending();
        assertEquals(2, jobs.count());
        verify(sender, times(1)).sendEmail(any(), eq(delivery.getDeduplicationKey()));
    }

    @Test
    void unavailableAgentDoesNotBlockTravelerAndCanBeRetriedLater() {
        Traveler traveler = traveler();
        Trip trip = new Trip();
        trip.setTitle("Lisbonne");
        trip.setTraveler(traveler);
        trip.setStatus(TripStatus.DRAFT);
        trip.setStartDate(LocalDate.now().plusDays(10));
        trip.setEndDate(LocalDate.now().plusDays(14));
        trip = trips.save(trip);
        Need need = new Need();
        need.setTrip(trip);
        need.setType(NeedType.FLIGHT);
        need.setStatus(NeedStatus.DRAFT);
        need = needs.save(need);
        requests.createBookingRequest(new CreateBookingRequest(need.getId(), "Vol direct"));
        outbox.processPendingEvents();
        processor.processPending();
        processor.processPending();

        assertEquals(1, deliveryJobs().size());
        assertEquals(EmailJob.Status.COMPLETED, deliveryJobs().getFirst().getStatus());
        EmailJob agentPreparation = jobs.findAll().stream()
            .filter(job -> job.getType().equals("AGENT:BOOKING_REQUESTED")).findFirst().orElseThrow();
        assertEquals(EmailJob.Status.PENDING, agentPreparation.getStatus());
        assertNotNull(agentPreparation.getLastError());

        Agent agent = new Agent();
        agent.setFirstName("Paul");
        agent.setLastName("Martin");
        agent.setEmail("agent-" + UUID.randomUUID() + "@example.com");
        agent.setStatus(AgentStatus.AVAILABLE);
        agents.saveAndFlush(agent);
        makeReady(agentPreparation.getId());
        processor.processPending();
        processor.processPending();

        assertEquals(2, deliveryJobs().size());
        assertTrue(deliveryJobs().stream().allMatch(job -> job.getStatus() == EmailJob.Status.COMPLETED));
        verify(sender, times(2)).sendEmail(any(), anyString());
    }

    @Test
    void failedDeliveryRetainsSnapshotAndIdempotencyKeyUntilAccepted() {
        EmailMessage message = new EmailMessage("alice@example.com", "Confirmation", "<p>Voyage</p>", "Voyage");
        EmailJob delivery = jobs.saveAndFlush(new EmailJob("retry-" + UUID.randomUUID(), "DELIVERY",
            mapper.writeValueAsString(message)));
        when(sender.sendEmail(any(), anyString()))
            .thenThrow(new EmailDeliveryException("Provider unavailable", null))
            .thenReturn("resend-retry-id");

        processor.processPending();
        EmailJob failed = jobs.findById(delivery.getId()).orElseThrow();
        assertEquals(EmailJob.Status.PENDING, failed.getStatus());
        assertEquals(1, failed.getAttempts());
        assertNull(failed.getProviderId());
        makeReady(delivery.getId());
        processor.processPending();

        EmailJob completed = jobs.findById(delivery.getId()).orElseThrow();
        assertEquals(EmailJob.Status.COMPLETED, completed.getStatus());
        assertEquals(2, completed.getAttempts());
        assertEquals("resend-retry-id", completed.getProviderId());
        assertEquals(delivery.getPayload(), completed.getPayload());
        verify(sender, times(2)).sendEmail(eq(message), eq(delivery.getDeduplicationKey()));
    }

    @Test
    void claimsAreExclusiveAndExpiredClaimsRecover() {
        EmailJob job = jobs.saveAndFlush(new EmailJob("lease-" + UUID.randomUUID(), "DELIVERY",
            mapper.writeValueAsString(new EmailMessage("a@example.com", "Hello", "<p>Hello</p>", "Hello"))));
        assertEquals(1, jobs.claim(job.getId(), Instant.now(), "worker-a"));
        assertEquals(0, jobs.claim(job.getId(), Instant.now(), "worker-b"));
        jdbc.update("update email_jobs set started_at = ? where id = ?",
            java.sql.Timestamp.from(Instant.now().minusSeconds(700)), job.getId());
        processor.processPending();
        assertEquals(EmailJob.Status.COMPLETED, jobs.findById(job.getId()).orElseThrow().getStatus());
        assertEquals(0, jobs.complete(job.getId(), "worker-a", Instant.now(), "stale-worker"));
    }

    @Test
    void paymentRecipientsAreIndependentAndEachAgentReceivesOnlyOneMessage() {
        Traveler traveler = traveler();
        Trip trip = new Trip();
        trip.setTraveler(traveler);
        trip.setTitle("Cuba");
        trip.setStatus(TripStatus.DRAFT);
        trip = trips.saveAndFlush(trip);
        Agent first = agent();
        Agent second = agent();
        request(trip, first, NeedType.FLIGHT);
        request(trip, first, NeedType.ACCOMMODATION);
        request(trip, second, NeedType.TRANSFER);
        PaymentSucceededEvent event = new PaymentSucceededEvent(1L, trip.getId(), traveler.getId(),
            new BigDecimal("25"), "EUR");
        queue.enqueue(123L, "PAYMENT_SUCCEEDED", mapper.writeValueAsString(event));
        processor.processPending();
        assertEquals(3, deliveryJobs().size());
        AtomicBoolean failFirst = new AtomicBoolean(true);
        when(sender.sendEmail(any(), anyString())).thenAnswer(invocation -> {
            EmailMessage message = invocation.getArgument(0);
            if (message.to().equals(first.getEmail()) && failFirst.getAndSet(false)) {
                throw new EmailDeliveryException("Temporary provider failure", null);
            }
            return "accepted-" + message.to();
        });
        processor.processPending();
        assertEquals(2, deliveryJobs().stream().filter(job -> job.getStatus() == EmailJob.Status.COMPLETED).count());
        EmailJob pending = deliveryJobs().stream()
            .filter(job -> job.getStatus() == EmailJob.Status.PENDING).findFirst().orElseThrow();
        makeReady(pending.getId());
        processor.processPending();
        assertTrue(deliveryJobs().stream().allMatch(job -> job.getStatus() == EmailJob.Status.COMPLETED));
        verify(sender, times(4)).sendEmail(any(), anyString());
    }

    @Test
    void threeAssignmentsForOneTripProduceOneEmailEvenAcrossAgentsAndRetries() {
        Traveler traveler = traveler();
        Trip trip = new Trip();
        trip.setTraveler(traveler);
        trip.setTitle("Lisbonne");
        trip.setStatus(TripStatus.DRAFT);
        trip = trips.saveAndFlush(trip);
        Agent firstAgent = agent();
        Agent secondAgent = agent();
        BookingRequest first = request(trip, firstAgent, NeedType.FLIGHT);
        BookingRequest second = request(trip, firstAgent, NeedType.ACCOMMODATION);
        BookingRequest third = request(trip, secondAgent, NeedType.TRANSFER);
        queue.enqueue(301L, "BOOKING_ASSIGNED", mapper.writeValueAsString(
            new BookingAssignedEvent(first.getId(), traveler.getId(), firstAgent.getId())));
        processor.processPending();
        assertEquals(1, deliveryJobs().size());
        when(sender.sendEmail(any(), anyString()))
            .thenThrow(new EmailDeliveryException("Temporary provider failure", null))
            .thenReturn("assignment-confirmation");
        processor.processPending();
        EmailJob delivery = deliveryJobs().getFirst();
        assertEquals(EmailJob.Status.PENDING, delivery.getStatus());

        queue.enqueue(302L, "BOOKING_ASSIGNED", mapper.writeValueAsString(
            new BookingAssignedEvent(second.getId(), traveler.getId(), firstAgent.getId())));
        queue.enqueue(303L, "BOOKING_ASSIGNED", mapper.writeValueAsString(
            new BookingAssignedEvent(third.getId(), traveler.getId(), secondAgent.getId())));
        processor.processPending();
        assertEquals(1, deliveryJobs().size());
        makeReady(delivery.getId());
        processor.processPending();
        BookingRequest later = request(trip, secondAgent, NeedType.CAR);
        queue.enqueue(304L, "BOOKING_ASSIGNED", mapper.writeValueAsString(
            new BookingAssignedEvent(later.getId(), traveler.getId(), secondAgent.getId())));
        processor.processPending();
        assertEquals(1, deliveryJobs().size());
        assertEquals(EmailJob.Status.COMPLETED, jobs.findById(delivery.getId()).orElseThrow().getStatus());
        verify(sender, times(2)).sendEmail(any(), eq(delivery.getDeduplicationKey()));
        EmailMessage message = mapper.readValue(delivery.getPayload(), EmailMessage.class);
        assertTrue(message.textBody().contains("commence à vous accompagner"));
        assertFalse(message.textBody().contains("Demande #"));
        assertTrue(message.textBody().contains("/traveler/trips/" + trip.getId()));
        assertTrue(jobs.findAll().stream().filter(job -> job.getType().equals("TRAVELER:BOOKING_ASSIGNED"))
            .allMatch(job -> job.getStatus() == EmailJob.Status.COMPLETED));
        assertEquals(BookingRequestStatus.IN_PROGRESS, requestRepository.findById(third.getId()).orElseThrow().getStatus());
    }

    @Test
    void anotherTripGetsItsOwnAssignmentConfirmation() {
        Traveler traveler = traveler();
        Agent agent = agent();
        for (int i = 0; i < 2; i++) {
            Trip trip = new Trip();
            trip.setTraveler(traveler);
            trip.setTitle("Voyage " + i);
            trip.setStatus(TripStatus.DRAFT);
            trip = trips.saveAndFlush(trip);
            BookingRequest request = request(trip, agent, NeedType.FLIGHT);
            queue.enqueue(400L + i, "BOOKING_ASSIGNED", mapper.writeValueAsString(
                new BookingAssignedEvent(request.getId(), traveler.getId(), agent.getId())));
        }
        processor.processPending();
        processor.processPending();
        assertEquals(2, deliveryJobs().size());
        assertTrue(deliveryJobs().stream().allMatch(job -> job.getStatus() == EmailJob.Status.COMPLETED));
        verify(sender, times(2)).sendEmail(any(), anyString());
    }

    @Test
    void confirmationInstructionsAndRejectionUseTheirOwnDurableMessages() {
        Traveler traveler = traveler();
        Trip trip = new Trip();
        trip.setTraveler(traveler);
        trip.setTitle("Lisbonne");
        trip.setStatus(TripStatus.DRAFT);
        trip = trips.saveAndFlush(trip);
        Agent agent = agent();
        BookingRequest request = request(trip, agent, NeedType.FLIGHT);
        queue.enqueue(200L, "BOOKING_CONFIRMED", mapper.writeValueAsString(
            new BookingConfirmedEvent(1L, traveler.getId(), trip.getId(), "Lisbonne", "PNR-123")));
        queue.enqueue(201L, "TRIP_BOOKINGS_CONFIRMED", mapper.writeValueAsString(
            new TripBookingsConfirmedEvent(trip.getId(), traveler.getId(), "Lisbonne", 2)));
        queue.enqueue(202L, "PROVIDER_PAYMENT_REQUIRED", mapper.writeValueAsString(
            new ProviderPaymentRequiredEvent(1L, traveler.getId(), trip.getId(), "Lisbonne")));
        queue.enqueue(203L, "QUOTE_REJECTED", mapper.writeValueAsString(
            new QuoteRejectedEvent(1L, request.getId(), traveler.getId(), agent.getId())));
        processor.processPending();
        assertEquals(3, deliveryJobs().size());
        processor.processPending();
        List<EmailMessage> messages = deliveryJobs().stream()
            .map(job -> mapper.readValue(job.getPayload(), EmailMessage.class)).toList();
        assertTrue(messages.stream().anyMatch(message -> message.textBody().contains("les 2 réservations")));
        assertTrue(messages.stream().noneMatch(message -> message.textBody().contains("PNR-123")));
        assertTrue(messages.stream().anyMatch(message -> message.textBody().contains("distinct des frais")));
        assertTrue(messages.stream().anyMatch(message -> message.subject().contains("refusée")));
        assertTrue(deliveryJobs().stream().allMatch(job -> job.getProviderId() != null));
    }

    @Test
    void travelerIsEmailedOnceOnlyWhenEveryActiveServiceOfTheTripIsBooked() {
        Traveler traveler = traveler();
        Trip trip = new Trip();
        trip.setTraveler(traveler);
        trip.setTitle("Lisbonne");
        trip.setStatus(TripStatus.CONFIRMED);
        trip = trips.saveAndFlush(trip);
        Agent agent = agent();
        Booking flight = pendingBooking(request(trip, agent, NeedType.FLIGHT), "PNR-FLIGHT");
        Booking hotel = pendingBooking(request(trip, agent, NeedType.ACCOMMODATION), "PNR-HOTEL");
        BookingRequest cancelled = request(trip, agent, NeedType.TRANSFER);
        cancelled.setStatus(BookingRequestStatus.CANCELLED);
        requestRepository.saveAndFlush(cancelled);

        bookingService.confirmBooking(flight.getId(), agent.getId());
        outbox.processPendingEvents();
        processor.processPending();
        processor.processPending();
        assertTrue(deliveryJobs().isEmpty());
        verifyNoInteractions(sender);

        bookingService.confirmBooking(hotel.getId(), agent.getId());
        outbox.processPendingEvents();
        processor.processPending();
        processor.processPending();
        assertEquals(1, deliveryJobs().size());
        EmailMessage message = mapper.readValue(deliveryJobs().getFirst().getPayload(), EmailMessage.class);
        assertEquals(traveler.getEmail(), message.to());
        assertTrue(message.subject().contains("Votre voyage est réservé"));
        assertTrue(message.textBody().contains("les 2 réservations"));
        assertEquals(1, events.findAll().stream()
            .filter(event -> event.getEventType().equals("TRIP_BOOKINGS_CONFIRMED")).count());
        verify(sender, times(1)).sendEmail(any(), anyString());
    }

    @Test
    void legacyUnknownProviderPaymentStatusIsReadAsNotDeterminedYet() {
        Traveler traveler = traveler();
        Trip trip = new Trip();
        trip.setTraveler(traveler);
        trip.setTitle("Lisbonne");
        trip.setStatus(TripStatus.CONFIRMED);
        trip = trips.saveAndFlush(trip);
        Booking booking = pendingBooking(request(trip, agent(), NeedType.FLIGHT), "PNR-LEGACY");
        // Simulate a pre-existing database whose constraint still allows the removed value.
        jdbc.execute("alter table bookings drop constraint if exists bookings_provider_payment_status_check");
        jdbc.update("update bookings set provider_payment_status = 'UNKNOWN' where id = ?", booking.getId());
        entityManager.clear();

        assertEquals(ProviderPaymentStatus.NOT_REQUIRED_YET,
            bookingRepository.findById(booking.getId()).orElseThrow().getProviderPaymentStatus());
    }

    private Booking pendingBooking(BookingRequest request, String providerReference) {
        Quote quote = new Quote();
        quote.setBookingRequest(request);
        quote.setProvider("FAKE");
        quote.setExternalOfferId(UUID.randomUUID().toString());
        quote.setProviderPrice(new BigDecimal("100"));
        quote.setTotalAmount(new BigDecimal("100"));
        quote.setCurrency("EUR");
        quote.setStatus(QuoteStatus.ACCEPTED);
        quote.setCreatedAt(Instant.now());
        quote = quoteRepository.saveAndFlush(quote);
        Booking booking = new Booking();
        booking.setQuote(quote);
        booking.setStatus(BookingStatus.PENDING);
        booking.setCreatedAt(Instant.now());
        booking.setProviderReference(providerReference);
        booking.setProviderPaymentStatus(ProviderPaymentStatus.NOT_REQUIRED_YET);
        return bookingRepository.saveAndFlush(booking);
    }

    private Traveler traveler() {
        Traveler traveler = new Traveler("Alice", "Martin", "traveler-" + UUID.randomUUID() + "@example.com");
        traveler.setAuth0Subject("auth0|" + UUID.randomUUID());
        traveler.setPreferredLanguage("fr");
        return travelers.saveAndFlush(traveler);
    }

    private List<EmailJob> deliveryJobs() {
        return jobs.findAll().stream().filter(job -> job.getType().equals("DELIVERY")).toList();
    }

    private Agent agent() {
        Agent agent = new Agent();
        agent.setFirstName("Paul");
        agent.setEmail("agent-" + UUID.randomUUID() + "@example.com");
        agent.setStatus(AgentStatus.AVAILABLE);
        return agents.saveAndFlush(agent);
    }

    private BookingRequest request(Trip trip, Agent agent, NeedType type) {
        Need need = new Need();
        need.setTrip(trip);
        need.setType(type);
        need.setStatus(NeedStatus.REQUESTED);
        need = needs.saveAndFlush(need);
        BookingRequest request = new BookingRequest();
        request.setNeed(need);
        request.setStatus(BookingRequestStatus.IN_PROGRESS);
        request.setAssignedAgent(agent);
        return requestRepository.saveAndFlush(request);
    }

    private void makeReady(Long id) {
        jdbc.update("update email_jobs set next_attempt_at = ? where id = ?",
            java.sql.Timestamp.from(Instant.now().minusSeconds(1)), id);
    }
}
