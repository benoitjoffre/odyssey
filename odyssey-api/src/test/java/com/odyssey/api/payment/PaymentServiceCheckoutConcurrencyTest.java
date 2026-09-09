package com.odyssey.api.payment;

import com.odyssey.api.agent.Agent;
import com.odyssey.api.agent.AgentRepository;
import com.odyssey.api.agent.AgentStatus;
import com.odyssey.api.booking.BookingRequestResponse;
import com.odyssey.api.booking.BookingRequestService;
import com.odyssey.api.booking.CreateBookingRequest;
import com.odyssey.api.need.Need;
import com.odyssey.api.need.NeedRepository;
import com.odyssey.api.need.NeedStatus;
import com.odyssey.api.need.NeedType;
import com.odyssey.api.payment.stripe.StripeCheckoutSession;
import com.odyssey.api.payment.stripe.StripeCheckoutSessionRequest;
import com.odyssey.api.payment.stripe.StripeGateway;
import com.odyssey.api.quote.CreateQuoteRequest;
import com.odyssey.api.quote.QuoteResponse;
import com.odyssey.api.quote.QuoteService;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerRepository;
import com.odyssey.api.trip.Trip;
import com.odyssey.api.trip.TripRepository;
import com.odyssey.api.trip.TripStatus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Reproduces two HTTP requests racing to create a Checkout Session for the
 * SAME Quote at (almost) the same time, and proves the fix in
 * {@link PaymentService#createCheckoutSession} — the Quote row lock
 * ({@code QuoteRepository.findByIdForUpdate}) — serializes them so that
 * only ONE {@link Payment} row is ever created for that Quote.
 *
 * <p>Not {@code @Transactional}: each concurrent call must run in its own
 * real transaction/connection to actually exercise Postgres row locking
 * (a class-level {@code @Transactional} test would bind every thread's
 * work to a single connection/transaction and would not reproduce the
 * race at all). Fixture data is therefore created and cleaned up
 * explicitly instead of relying on test-managed rollback.</p>
 *
 * <p>No real Stripe network call is made: {@link StripeGateway} is
 * replaced by a Mockito mock via {@code @MockitoBean}.</p>
 */
@SpringBootTest
@TestPropertySource(properties = "stripe.secret-key=sk_test_dummy")
class PaymentServiceCheckoutConcurrencyTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRequestService bookingRequestService;

    @Autowired
    private QuoteService quoteService;

    @Autowired
    private NeedRepository needRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TravelerRepository travelerRepository;

    @Autowired
    private AgentRepository agentRepository;

    @MockitoBean
    private StripeGateway stripeGateway;

    private Long createdQuoteId;
    private Long createdTripId;
    private Long createdTravelerId;
    private Long createdAgentId;

    @AfterEach
    void cleanUp() {
        // Payment is not part of the Trip -> Need -> BookingRequest -> Quote
        // cascade (Payment has no mapped inverse collection on Quote), so it
        // must be deleted explicitly before the cascading Trip deletion
        // below removes the Quote it points to.
        if (createdQuoteId != null) {
            paymentRepository.findByQuoteIdOrderByCreatedAtDesc(createdQuoteId)
                .forEach(paymentRepository::delete);
        }
        // Deleting the Trip cascades (CascadeType.REMOVE) down through Need,
        // BookingRequest and Quote in one go, exactly like the app's own
        // trip-deletion flow (see TripDeletionIntegrationTest) — avoids
        // manually deleting each child out of order.
        if (createdTripId != null) {
            tripRepository.deleteById(createdTripId);
        }
        if (createdTravelerId != null) {
            travelerRepository.deleteById(createdTravelerId);
        }
        if (createdAgentId != null) {
            agentRepository.deleteById(createdAgentId);
        }
    }

    @Test
    void concurrentCheckoutRequestsForSameQuoteCreateOnlyOnePayment() throws Exception {

        when(stripeGateway.createCheckoutSession(any(StripeCheckoutSessionRequest.class)))
            .thenAnswer(invocation -> new StripeCheckoutSession(
                "cs_test_" + UUID.randomUUID(),
                "https://checkout.stripe.com/test-session"
            ));

        Traveler traveler = travelerRepository.save(new Traveler(
            "Concurrency",
            "Test",
            "concurrency-" + UUID.randomUUID() + "@example.com"
        ));
        createdTravelerId = traveler.getId();

        Trip trip = new Trip();
        trip.setTitle("Concurrency test trip");
        trip.setStartDate(LocalDate.now().plusDays(1));
        trip.setEndDate(LocalDate.now().plusDays(2));
        trip.setStatus(TripStatus.DRAFT);
        trip.setTraveler(traveler);
        trip = tripRepository.save(trip);
        createdTripId = trip.getId();

        Need need = new Need();
        need.setType(NeedType.CAR);
        need.setStatus(NeedStatus.DRAFT);
        need.setTrip(trip);
        need = needRepository.save(need);

        agentRepository.findByStatus(AgentStatus.AVAILABLE)
            .forEach(existing -> {
                existing.setStatus(AgentStatus.BUSY);
                agentRepository.save(existing);
            });
        Agent agent = new Agent();
        agent.setFirstName("Concurrency");
        agent.setLastName("Agent");
        agent.setEmail("concurrency-agent-" + UUID.randomUUID() + "@example.com");
        agent.setStatus(AgentStatus.AVAILABLE);
        agent = agentRepository.save(agent);
        createdAgentId = agent.getId();

        BookingRequestResponse bookingRequest = bookingRequestService
            .createBookingRequest(new CreateBookingRequest(need.getId(), "notes"));

        bookingRequestService.claimBookingRequest(bookingRequest.id(), agent.getId());

        QuoteResponse quote = quoteService.createQuote(
            bookingRequest.id(),
            agent.getId(),
            new CreateQuoteRequest(
                "provider",
                "offer-1",
                BigDecimal.valueOf(670),
                BigDecimal.valueOf(100),
                "EUR",
                "Concurrency test quote",
                null
            )
        );
        createdQuoteId = quote.id();

        quoteService.sendQuote(quote.id(), agent.getId());
        quoteService.acceptQuote(quote.id(), traveler.getId());

        int concurrentRequests = 8;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch readyLatch = new CountDownLatch(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();

        List<Future<?>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < concurrentRequests; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    paymentService.createCheckoutSession(quote.id(), traveler.getId());
                    successCount.incrementAndGet();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
                return null;
            }));
        }

        readyLatch.await(10, TimeUnit.SECONDS);
        startLatch.countDown();

        for (Future<?> future : futures) {
            future.get(15, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertEquals(concurrentRequests, successCount.get());

        List<Payment> payments = paymentRepository.findByQuoteIdOrderByCreatedAtDesc(quote.id());
        assertEquals(1, payments.size());
    }
}
