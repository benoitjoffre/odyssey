package com.odyssey.api.quote;

import com.odyssey.api.agent.Agent;
import com.odyssey.api.agent.AgentRepository;
import com.odyssey.api.booking.BookingRequest;
import com.odyssey.api.booking.BookingRequestRepository;
import com.odyssey.api.booking.BookingRequestStatus;
import com.odyssey.api.booking.confirmation.BookingRepository;
import com.odyssey.api.need.Need;
import com.odyssey.api.outbox.OutboxEventRepository;
import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.payment.Payment;
import com.odyssey.api.payment.PaymentRepository;
import com.odyssey.api.payment.PaymentStatus;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerRepository;
import com.odyssey.api.trip.Trip;
import com.odyssey.api.trip.TripRepository;
import com.odyssey.api.trip.TripStatus;
import com.odyssey.api.outbox.OutboxEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@code totalAmount} is always computed server-side from
 * {@code providerPrice + assistanceFee}, never trusted from the caller,
 * and that the traveler-facing response carries the payment status
 * looked up from the {@link PaymentRepository}.
 */
@ExtendWith(MockitoExtension.class)
class QuoteServiceTest {

    private static final Long TRIP_ID = 77L;

    @Mock
    private QuoteRepository quoteRepository;

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private BookingRequestRepository bookingRequestRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private TravelerRepository travelerRepository;

    @Mock
    private TripRepository tripRepository;

    private QuoteService quoteService;

    @BeforeEach
    void setUp() {
        quoteService = new QuoteService(
            quoteRepository,
            agentRepository,
            travelerRepository,
            bookingRequestRepository,
            outboxEventRepository,
            paymentRepository,
            bookingRepository,
            tripRepository,
            new ObjectMapper()
        );
    }

    private BookingRequest inProgressBookingRequestAssignedTo(Long agentId) {
        Agent agent = new Agent();
        ReflectionTestUtils.setField(agent, "id", agentId);

        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setStatus(BookingRequestStatus.IN_PROGRESS);
        bookingRequest.setAssignedAgent(agent);
        attachToTrip(bookingRequest);
        return bookingRequest;
    }

    private void attachToTrip(BookingRequest bookingRequest) {
        Trip trip = new Trip();
        ReflectionTestUtils.setField(trip, "id", TRIP_ID);

        Need need = new Need();
        need.setTrip(trip);
        bookingRequest.setNeed(need);
    }

    @Test
    void createQuoteComputesTotalAmountAsProviderPricePlusAssistanceFee() {

        Long bookingRequestId = 1L;
        Long agentId = 2L;
        BookingRequest bookingRequest = inProgressBookingRequestAssignedTo(agentId);

        when(bookingRequestRepository.findById(bookingRequestId))
            .thenReturn(Optional.of(bookingRequest));
        when(quoteRepository.save(any(Quote.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CreateQuoteRequest request = new CreateQuoteRequest(
            "test-provider",
            "offer-1",
            BigDecimal.valueOf(670),
            BigDecimal.valueOf(100),
            "EUR",
            "Hôtel test",
            null
        );

        QuoteResponse response = quoteService.createQuote(bookingRequestId, agentId, request);

        assertEquals(0, BigDecimal.valueOf(670).compareTo(response.providerPrice()));
        assertEquals(0, BigDecimal.valueOf(100).compareTo(response.assistanceFee()));
        assertEquals(0, BigDecimal.valueOf(770).compareTo(response.totalAmount()));

        ArgumentCaptor<Quote> savedQuoteCaptor = ArgumentCaptor.forClass(Quote.class);
        verify(quoteRepository).save(savedQuoteCaptor.capture());

        Quote savedQuote = savedQuoteCaptor.getValue();
        assertEquals(QuoteStatus.DRAFT, savedQuote.getStatus());
        assertEquals(0, BigDecimal.valueOf(770).compareTo(savedQuote.getTotalAmount()));
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void createQuoteRejectsNegativeProviderPrice() {

        Long bookingRequestId = 1L;
        Long agentId = 2L;
        BookingRequest bookingRequest = inProgressBookingRequestAssignedTo(agentId);

        when(bookingRequestRepository.findById(bookingRequestId))
            .thenReturn(Optional.of(bookingRequest));

        CreateQuoteRequest request = new CreateQuoteRequest(
            "test-provider",
            "offer-1",
            BigDecimal.valueOf(-1),
            BigDecimal.valueOf(100),
            "EUR",
            "Hôtel test",
            null
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> quoteService.createQuote(bookingRequestId, agentId, request)
        );
        verify(quoteRepository, never()).save(any());
    }

    @Test
    void createQuoteRejectsNegativeAssistanceFee() {

        Long bookingRequestId = 1L;
        Long agentId = 2L;
        BookingRequest bookingRequest = inProgressBookingRequestAssignedTo(agentId);

        when(bookingRequestRepository.findById(bookingRequestId))
            .thenReturn(Optional.of(bookingRequest));

        CreateQuoteRequest request = new CreateQuoteRequest(
            "test-provider",
            "offer-1",
            BigDecimal.valueOf(670),
            BigDecimal.valueOf(-1),
            "EUR",
            "Hôtel test",
            null
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> quoteService.createQuote(bookingRequestId, agentId, request)
        );
        verify(quoteRepository, never()).save(any());
    }

    @Test
    void assignedAgentSeesProviderPriceBeforeTravelerAcceptance() {
        Long bookingRequestId = 1L;
        Long agentId = 2L;
        String auth0Subject = "auth0|agent";
        BookingRequest bookingRequest = inProgressBookingRequestAssignedTo(agentId);

        Agent agent = bookingRequest.getAssignedAgent();
        when(agentRepository.findByAuth0Subject(auth0Subject))
            .thenReturn(Optional.of(agent));
        when(bookingRequestRepository.findById(bookingRequestId))
            .thenReturn(Optional.of(bookingRequest));

        Quote draftQuote = new Quote();
        ReflectionTestUtils.setField(draftQuote, "id", 42L);
        draftQuote.setBookingRequest(bookingRequest);
        draftQuote.setProvider("test-provider");
        draftQuote.setProviderPrice(BigDecimal.valueOf(670));
        draftQuote.setAssistanceFee(BigDecimal.valueOf(100));
        draftQuote.setTotalAmount(BigDecimal.valueOf(770));
        draftQuote.setCurrency("EUR");
        draftQuote.setStatus(QuoteStatus.DRAFT);
        when(quoteRepository.findByBookingRequestIdOrderByIdDesc(bookingRequestId))
            .thenReturn(java.util.List.of(draftQuote));

        var responses = quoteService.getQuotesByBookingRequest(
            bookingRequestId,
            auth0Subject
        );

        assertEquals(1, responses.size());
        assertEquals(QuoteStatus.DRAFT, responses.get(0).status());
        assertEquals("test-provider", responses.get(0).provider());
        assertEquals(0, BigDecimal.valueOf(670).compareTo(responses.get(0).providerPrice()));
        assertEquals(0, BigDecimal.valueOf(100).compareTo(responses.get(0).assistanceFee()));
        assertEquals(0, BigDecimal.valueOf(770).compareTo(responses.get(0).totalAmount()));
    }

    @Test
    void agentCannotSeeQuotesFromAnotherAgentsBookingRequest() {
        Long bookingRequestId = 1L;
        String auth0Subject = "auth0|agent";
        Agent currentAgent = new Agent();
        ReflectionTestUtils.setField(currentAgent, "id", 2L);
        BookingRequest bookingRequest = inProgressBookingRequestAssignedTo(3L);

        when(agentRepository.findByAuth0Subject(auth0Subject))
            .thenReturn(Optional.of(currentAgent));
        when(bookingRequestRepository.findById(bookingRequestId))
            .thenReturn(Optional.of(bookingRequest));

        assertThrows(
            ResourceNotFoundException.class,
            () -> quoteService.getQuotesByBookingRequest(bookingRequestId, auth0Subject)
        );
        verify(quoteRepository, never()).findByBookingRequestIdOrderByIdDesc(any());
    }

    @Test
    void sendDraftQuotesForTripSendsAllDraftsAndCreatesOneOutboxEvent() {
        Long tripId = 10L;
        String auth0Subject = "auth0|agent";
        Agent agent = new Agent();
        ReflectionTestUtils.setField(agent, "id", 2L);
        Traveler traveler = new Traveler();
        ReflectionTestUtils.setField(traveler, "id", 5L);
        Trip trip = new Trip();
        ReflectionTestUtils.setField(trip, "id", tripId);
        trip.setTraveler(traveler);

        BookingRequest firstRequest = inProgressBookingRequestAssignedTo(2L);
        BookingRequest secondRequest = inProgressBookingRequestAssignedTo(2L);
        Quote firstDraft = quote(101L, firstRequest, QuoteStatus.DRAFT);
        Quote secondDraft = quote(102L, secondRequest, QuoteStatus.DRAFT);

        when(agentRepository.findByAuth0Subject(auth0Subject)).thenReturn(Optional.of(agent));
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(quoteRepository.findByBookingRequestNeedTripIdAndStatusOrderByIdAsc(
            tripId,
            QuoteStatus.DRAFT
        )).thenReturn(List.of(firstDraft, secondDraft));
        when(quoteRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = quoteService.sendDraftQuotesForTrip(tripId, auth0Subject);

        assertEquals(List.of(101L, 102L), response.sentQuoteIds());
        assertEquals(QuoteStatus.SENT, firstDraft.getStatus());
        assertEquals(QuoteStatus.SENT, secondDraft.getStatus());
        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        assertEquals("TRIP_QUOTES_SENT", outboxCaptor.getValue().getEventType());
    }

    @Test
    void sendDraftQuotesForTripDoesNothingWhenNoDraftExists() {
        Long tripId = 10L;
        String auth0Subject = "auth0|agent";
        Agent agent = new Agent();
        ReflectionTestUtils.setField(agent, "id", 2L);
        Trip trip = new Trip();
        ReflectionTestUtils.setField(trip, "id", tripId);

        when(agentRepository.findByAuth0Subject(auth0Subject)).thenReturn(Optional.of(agent));
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(quoteRepository.findByBookingRequestNeedTripIdAndStatusOrderByIdAsc(
            tripId,
            QuoteStatus.DRAFT
        )).thenReturn(List.of());

        var response = quoteService.sendDraftQuotesForTrip(tripId, auth0Subject);

        assertEquals(List.of(), response.sentQuoteIds());
        verify(quoteRepository, never()).saveAll(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void getQuotesByTravelerExcludesDraftsAndIncludesLatestPaymentStatus() {

        Long travelerId = 5L;
        BookingRequest bookingRequest = new BookingRequest();
        ReflectionTestUtils.setField(bookingRequest, "id", 9L);
        attachToTrip(bookingRequest);

        Quote quote = new Quote();
        ReflectionTestUtils.setField(quote, "id", 42L);
        quote.setBookingRequest(bookingRequest);
        quote.setStatus(QuoteStatus.ACCEPTED);
        quote.setCurrency("EUR");
        quote.setProviderPrice(BigDecimal.valueOf(670));
        quote.setAssistanceFee(BigDecimal.valueOf(100));
        quote.setTotalAmount(BigDecimal.valueOf(770));

        when(quoteRepository.findByBookingRequestNeedTripTravelerIdAndStatusNot(
            travelerId, QuoteStatus.DRAFT
        )).thenReturn(java.util.List.of(quote));

        Payment payment = mock(Payment.class);
        when(payment.getStatus()).thenReturn(PaymentStatus.PAID);
        when(paymentRepository.findFirstByTripIdOrderByCreatedAtDesc(TRIP_ID))
            .thenReturn(Optional.of(payment));

        var responses = quoteService.getQuotesByTraveler(travelerId);

        assertEquals(1, responses.size());
        assertEquals(QuoteStatus.ACCEPTED, responses.get(0).status());
        assertEquals(PaymentStatus.PAID, responses.get(0).paymentStatus());
        verify(quoteRepository).findByBookingRequestNeedTripTravelerIdAndStatusNot(
            travelerId,
            QuoteStatus.DRAFT
        );
    }

    @Test
    void getQuotesByTravelerReturnsNullPaymentStatusWhenNoPaymentExists() {

        Long travelerId = 5L;
        BookingRequest bookingRequest = new BookingRequest();
        attachToTrip(bookingRequest);

        Quote quote = new Quote();
        ReflectionTestUtils.setField(quote, "id", 43L);
        quote.setBookingRequest(bookingRequest);
        quote.setStatus(QuoteStatus.SENT);
        quote.setCurrency("EUR");
        quote.setProviderPrice(BigDecimal.valueOf(670));
        quote.setAssistanceFee(BigDecimal.valueOf(100));
        quote.setTotalAmount(BigDecimal.valueOf(770));

        when(quoteRepository.findByBookingRequestNeedTripTravelerIdAndStatusNot(
            travelerId, QuoteStatus.DRAFT
        )).thenReturn(java.util.List.of(quote));
        when(paymentRepository.findFirstByTripIdOrderByCreatedAtDesc(TRIP_ID))
            .thenReturn(Optional.empty());

        var responses = quoteService.getQuotesByTraveler(travelerId);

        assertEquals(1, responses.size());
        assertNull(responses.get(0).paymentStatus());
    }

    @Test
    void getQuotesByCurrentTravelerFiltersByAuthenticatedTraveler() {
        String auth0Subject = "auth0|traveler-2";
        Traveler traveler = new Traveler();
        ReflectionTestUtils.setField(traveler, "id", 2L);
        when(travelerRepository.findByAuth0Subject(auth0Subject)).thenReturn(Optional.of(traveler));

        BookingRequest bookingRequest = new BookingRequest();
        ReflectiveHelper.setQuoteOwner(bookingRequest, 2L);

        Quote quote = new Quote();
        ReflectionTestUtils.setField(quote, "id", 77L);
        quote.setBookingRequest(bookingRequest);
        quote.setStatus(QuoteStatus.SENT);
        quote.setCurrency("EUR");
        quote.setProviderPrice(BigDecimal.valueOf(670));
        quote.setAssistanceFee(BigDecimal.valueOf(100));
        quote.setTotalAmount(BigDecimal.valueOf(770));

        when(quoteRepository.findByBookingRequestNeedTripTravelerIdAndStatusNot(2L, QuoteStatus.DRAFT))
            .thenReturn(java.util.List.of(quote));

        var responses = quoteService.getQuotesByCurrentTraveler(auth0Subject);

        assertEquals(1, responses.size());
        assertEquals(77L, responses.get(0).id());
    }

    @Test
    void travelerAcceptsSentQuoteAndCreatesAcceptedOutboxEvent() {
        String auth0Subject = "auth0|traveler-1";
        Traveler traveler = new Traveler();
        ReflectionTestUtils.setField(traveler, "id", 1L);
        when(travelerRepository.findByAuth0Subject(auth0Subject)).thenReturn(Optional.of(traveler));

        BookingRequest bookingRequest = inProgressBookingRequestAssignedTo(5L);
        ReflectionTestUtils.setField(bookingRequest, "id", 100L);
        ReflectiveHelper.setQuoteOwner(bookingRequest, 1L);
        bookingRequest.setAssignedAgent(agent(5L));
        Quote quote = pricedQuote(90L, bookingRequest, QuoteStatus.SENT);

        when(quoteRepository.findById(90L)).thenReturn(Optional.of(quote));
        when(quoteRepository.save(quote)).thenReturn(quote);

        var response = quoteService.acceptQuote(90L, auth0Subject);

        assertEquals(QuoteStatus.ACCEPTED, response.status());
        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        assertEquals("QUOTE_ACCEPTED", outboxCaptor.getValue().getEventType());
    }

    // --- Trip confirmation (E2E-01) --------------------------------------
    //
    // A Trip becomes CONFIRMED only when every active BookingRequest of the
    // Trip has a "current" Quote (highest id for that BookingRequest) whose
    // status is ACCEPTED. Historical Quotes (REJECTED/EXPIRED/superseded)
    // must never block confirmation.

    private BookingRequest bookingRequestOn(Trip trip, Long bookingRequestId) {
        BookingRequest bookingRequest = new BookingRequest();
        ReflectionTestUtils.setField(bookingRequest, "id", bookingRequestId);
        bookingRequest.setStatus(BookingRequestStatus.IN_PROGRESS);
        bookingRequest.setAssignedAgent(agent(5L));
        Need need = new Need();
        need.setTrip(trip);
        bookingRequest.setNeed(need);
        return bookingRequest;
    }

    private Trip tripOwnedBy(Traveler traveler) {
        Trip trip = new Trip();
        ReflectionTestUtils.setField(trip, "id", TRIP_ID);
        trip.setStatus(TripStatus.DRAFT);
        trip.setTraveler(traveler);
        return trip;
    }

    @Test
    void tripBecomesConfirmedWhenAllCurrentQuotesAreAccepted() {
        String auth0Subject = "auth0|traveler-1";
        Traveler traveler = new Traveler();
        ReflectionTestUtils.setField(traveler, "id", 1L);
        when(travelerRepository.findByAuth0Subject(auth0Subject)).thenReturn(Optional.of(traveler));

        Trip trip = tripOwnedBy(traveler);
        BookingRequest flightRequest = bookingRequestOn(trip, 201L);
        BookingRequest hotelRequest = bookingRequestOn(trip, 202L);

        Quote flightQuote = pricedQuote(1L, flightRequest, QuoteStatus.ACCEPTED);
        Quote hotelQuote = pricedQuote(2L, hotelRequest, QuoteStatus.SENT);

        when(quoteRepository.findById(2L)).thenReturn(Optional.of(hotelQuote));
        when(quoteRepository.save(hotelQuote)).thenReturn(hotelQuote);
        when(bookingRequestRepository.findByNeedTripId(TRIP_ID))
            .thenReturn(List.of(flightRequest, hotelRequest));
        when(quoteRepository.findFirstByBookingRequestIdOrderByIdDesc(201L))
            .thenReturn(Optional.of(flightQuote));
        when(quoteRepository.findFirstByBookingRequestIdOrderByIdDesc(202L))
            .thenReturn(Optional.of(hotelQuote));

        quoteService.acceptQuote(2L, auth0Subject);

        assertEquals(TripStatus.CONFIRMED, trip.getStatus());
    }

    @Test
    void tripBecomesConfirmedWhenHistoricalRejectedQuoteIsSupersededByAcceptedQuote() {
        // Reproduces E2E-01: TRANSFER Quote #30 REJECTED, then Quote #31
        // (higher id) SENT and now ACCEPTED. FLIGHT/HOTEL already ACCEPTED.
        String auth0Subject = "auth0|traveler-1";
        Traveler traveler = new Traveler();
        ReflectionTestUtils.setField(traveler, "id", 1L);
        when(travelerRepository.findByAuth0Subject(auth0Subject)).thenReturn(Optional.of(traveler));

        Trip trip = tripOwnedBy(traveler);
        BookingRequest flightRequest = bookingRequestOn(trip, 301L);
        BookingRequest hotelRequest = bookingRequestOn(trip, 302L);
        BookingRequest transferRequest = bookingRequestOn(trip, 303L);

        Quote flightQuote = pricedQuote(10L, flightRequest, QuoteStatus.ACCEPTED);
        Quote hotelQuote = pricedQuote(20L, hotelRequest, QuoteStatus.ACCEPTED);
        // Historical Quote, intentionally never stubbed on
        // findFirstByBookingRequestIdOrderByIdDesc: it must play no role at
        // all in the confirmation decision.
        Quote newTransferQuote = pricedQuote(31L, transferRequest, QuoteStatus.SENT);

        when(quoteRepository.findById(31L)).thenReturn(Optional.of(newTransferQuote));
        when(quoteRepository.save(newTransferQuote)).thenReturn(newTransferQuote);
        when(bookingRequestRepository.findByNeedTripId(TRIP_ID))
            .thenReturn(List.of(flightRequest, hotelRequest, transferRequest));
        when(quoteRepository.findFirstByBookingRequestIdOrderByIdDesc(301L))
            .thenReturn(Optional.of(flightQuote));
        when(quoteRepository.findFirstByBookingRequestIdOrderByIdDesc(302L))
            .thenReturn(Optional.of(hotelQuote));
        when(quoteRepository.findFirstByBookingRequestIdOrderByIdDesc(303L))
            .thenReturn(Optional.of(newTransferQuote));

        quoteService.acceptQuote(31L, auth0Subject);

        assertEquals(TripStatus.CONFIRMED, trip.getStatus());
        verify(quoteRepository, never()).findByBookingRequestNeedTripId(any());
    }

    @Test
    void tripBecomesConfirmedWhenHistoricalExpiredQuoteIsSupersededByAcceptedQuote() {
        String auth0Subject = "auth0|traveler-1";
        Traveler traveler = new Traveler();
        ReflectionTestUtils.setField(traveler, "id", 1L);
        when(travelerRepository.findByAuth0Subject(auth0Subject)).thenReturn(Optional.of(traveler));

        Trip trip = tripOwnedBy(traveler);
        BookingRequest flightRequest = bookingRequestOn(trip, 401L);
        BookingRequest transferRequest = bookingRequestOn(trip, 402L);

        Quote flightQuote = pricedQuote(40L, flightRequest, QuoteStatus.ACCEPTED);
        // Historical EXPIRED Quote #50 intentionally never stubbed as the
        // current one: only Quote #51 (higher id) must be considered.
        Quote newTransferQuote = pricedQuote(51L, transferRequest, QuoteStatus.SENT);

        when(quoteRepository.findById(51L)).thenReturn(Optional.of(newTransferQuote));
        when(quoteRepository.save(newTransferQuote)).thenReturn(newTransferQuote);
        when(bookingRequestRepository.findByNeedTripId(TRIP_ID))
            .thenReturn(List.of(flightRequest, transferRequest));
        when(quoteRepository.findFirstByBookingRequestIdOrderByIdDesc(401L))
            .thenReturn(Optional.of(flightQuote));
        when(quoteRepository.findFirstByBookingRequestIdOrderByIdDesc(402L))
            .thenReturn(Optional.of(newTransferQuote));

        quoteService.acceptQuote(51L, auth0Subject);

        assertEquals(TripStatus.CONFIRMED, trip.getStatus());
    }

    @Test
    void tripNotConfirmedWhenAnotherBookingRequestsCurrentQuoteIsStillSent() {
        String auth0Subject = "auth0|traveler-1";
        Traveler traveler = new Traveler();
        ReflectionTestUtils.setField(traveler, "id", 1L);
        when(travelerRepository.findByAuth0Subject(auth0Subject)).thenReturn(Optional.of(traveler));

        Trip trip = tripOwnedBy(traveler);
        BookingRequest hotelRequest = bookingRequestOn(trip, 501L);
        BookingRequest transferRequest = bookingRequestOn(trip, 502L);

        Quote hotelQuote = pricedQuote(60L, hotelRequest, QuoteStatus.SENT);
        Quote transferQuote = pricedQuote(61L, transferRequest, QuoteStatus.SENT);

        when(quoteRepository.findById(60L)).thenReturn(Optional.of(hotelQuote));
        when(quoteRepository.save(hotelQuote)).thenReturn(hotelQuote);
        when(bookingRequestRepository.findByNeedTripId(TRIP_ID))
            .thenReturn(List.of(hotelRequest, transferRequest));
        when(quoteRepository.findFirstByBookingRequestIdOrderByIdDesc(501L))
            .thenReturn(Optional.of(hotelQuote));
        when(quoteRepository.findFirstByBookingRequestIdOrderByIdDesc(502L))
            .thenReturn(Optional.of(transferQuote)); // still SENT, not accepted

        quoteService.acceptQuote(60L, auth0Subject);

        assertEquals(QuoteStatus.ACCEPTED, hotelQuote.getStatus());
        assertEquals(TripStatus.DRAFT, trip.getStatus());
    }

    @Test
    void tripNotConfirmedWhenAnotherBookingRequestsCurrentQuoteIsStillDraft() {
        String auth0Subject = "auth0|traveler-1";
        Traveler traveler = new Traveler();
        ReflectionTestUtils.setField(traveler, "id", 1L);
        when(travelerRepository.findByAuth0Subject(auth0Subject)).thenReturn(Optional.of(traveler));

        Trip trip = tripOwnedBy(traveler);
        BookingRequest hotelRequest = bookingRequestOn(trip, 601L);
        BookingRequest transferRequest = bookingRequestOn(trip, 602L);

        Quote hotelQuote = pricedQuote(70L, hotelRequest, QuoteStatus.SENT);
        Quote draftTransferQuote = pricedQuote(71L, transferRequest, QuoteStatus.DRAFT);

        when(quoteRepository.findById(70L)).thenReturn(Optional.of(hotelQuote));
        when(quoteRepository.save(hotelQuote)).thenReturn(hotelQuote);
        when(bookingRequestRepository.findByNeedTripId(TRIP_ID))
            .thenReturn(List.of(hotelRequest, transferRequest));
        when(quoteRepository.findFirstByBookingRequestIdOrderByIdDesc(601L))
            .thenReturn(Optional.of(hotelQuote));
        when(quoteRepository.findFirstByBookingRequestIdOrderByIdDesc(602L))
            .thenReturn(Optional.of(draftTransferQuote));

        quoteService.acceptQuote(70L, auth0Subject);

        assertEquals(TripStatus.DRAFT, trip.getStatus());
    }

    @Test
    void tripNotConfirmedWhenABookingRequestHasNoQuoteAtAll() {
        String auth0Subject = "auth0|traveler-1";
        Traveler traveler = new Traveler();
        ReflectionTestUtils.setField(traveler, "id", 1L);
        when(travelerRepository.findByAuth0Subject(auth0Subject)).thenReturn(Optional.of(traveler));

        Trip trip = tripOwnedBy(traveler);
        BookingRequest hotelRequest = bookingRequestOn(trip, 701L);
        BookingRequest transferRequest = bookingRequestOn(trip, 702L);

        Quote hotelQuote = pricedQuote(80L, hotelRequest, QuoteStatus.SENT);

        when(quoteRepository.findById(80L)).thenReturn(Optional.of(hotelQuote));
        when(quoteRepository.save(hotelQuote)).thenReturn(hotelQuote);
        when(bookingRequestRepository.findByNeedTripId(TRIP_ID))
            .thenReturn(List.of(hotelRequest, transferRequest));
        when(quoteRepository.findFirstByBookingRequestIdOrderByIdDesc(701L))
            .thenReturn(Optional.of(hotelQuote));
        when(quoteRepository.findFirstByBookingRequestIdOrderByIdDesc(702L))
            .thenReturn(Optional.empty()); // TRANSFER has no Quote yet

        quoteService.acceptQuote(80L, auth0Subject);

        assertEquals(TripStatus.DRAFT, trip.getStatus());
    }

    @Test
    void tripConfirmationOnlyConsidersHighestIdQuotePerBookingRequest() {
        // Mixed history on ONE BookingRequest: ACCEPTED (#1) then REJECTED
        // (#2) then ACCEPTED again (#3, the highest id). Only #3 matters.
        String auth0Subject = "auth0|traveler-1";
        Traveler traveler = new Traveler();
        ReflectionTestUtils.setField(traveler, "id", 1L);
        when(travelerRepository.findByAuth0Subject(auth0Subject)).thenReturn(Optional.of(traveler));

        Trip trip = tripOwnedBy(traveler);
        BookingRequest flightRequest = bookingRequestOn(trip, 801L);
        BookingRequest hotelRequest = bookingRequestOn(trip, 802L);

        Quote flightCurrentQuote = pricedQuote(93L, flightRequest, QuoteStatus.ACCEPTED);
        Quote hotelQuote = pricedQuote(94L, hotelRequest, QuoteStatus.SENT);

        when(quoteRepository.findById(94L)).thenReturn(Optional.of(hotelQuote));
        when(quoteRepository.save(hotelQuote)).thenReturn(hotelQuote);
        when(bookingRequestRepository.findByNeedTripId(TRIP_ID))
            .thenReturn(List.of(flightRequest, hotelRequest));
        when(quoteRepository.findFirstByBookingRequestIdOrderByIdDesc(801L))
            .thenReturn(Optional.of(flightCurrentQuote));
        when(quoteRepository.findFirstByBookingRequestIdOrderByIdDesc(802L))
            .thenReturn(Optional.of(hotelQuote));

        quoteService.acceptQuote(94L, auth0Subject);

        assertEquals(TripStatus.CONFIRMED, trip.getStatus());
    }

    @Test
    void bothAcceptQuoteOverloadsApplySameTripConfirmationLogic() {
        // Same scenario as tripBecomesConfirmedWhenAllCurrentQuotesAreAccepted
        // but going through the (quoteId, travelerId) overload used by
        // internal/integration callers, proving both entry points share the
        // exact same Trip confirmation rule.
        Traveler traveler = new Traveler();
        ReflectionTestUtils.setField(traveler, "id", 1L);

        Trip trip = tripOwnedBy(traveler);
        BookingRequest flightRequest = bookingRequestOn(trip, 901L);
        BookingRequest hotelRequest = bookingRequestOn(trip, 902L);

        Quote flightQuote = pricedQuote(95L, flightRequest, QuoteStatus.ACCEPTED);
        Quote hotelQuote = pricedQuote(96L, hotelRequest, QuoteStatus.SENT);

        when(quoteRepository.findById(96L)).thenReturn(Optional.of(hotelQuote));
        when(quoteRepository.save(hotelQuote)).thenReturn(hotelQuote);
        when(bookingRequestRepository.findByNeedTripId(TRIP_ID))
            .thenReturn(List.of(flightRequest, hotelRequest));
        when(quoteRepository.findFirstByBookingRequestIdOrderByIdDesc(901L))
            .thenReturn(Optional.of(flightQuote));
        when(quoteRepository.findFirstByBookingRequestIdOrderByIdDesc(902L))
            .thenReturn(Optional.of(hotelQuote));

        quoteService.acceptQuote(96L, 1L);

        assertEquals(TripStatus.CONFIRMED, trip.getStatus());
    }

    @Test
    void travelerRejectsSentQuoteAndCreatesRejectedOutboxEvent() {
        String auth0Subject = "auth0|traveler-1";
        Traveler traveler = new Traveler();
        ReflectionTestUtils.setField(traveler, "id", 1L);
        when(travelerRepository.findByAuth0Subject(auth0Subject)).thenReturn(Optional.of(traveler));

        BookingRequest bookingRequest = inProgressBookingRequestAssignedTo(5L);
        ReflectionTestUtils.setField(bookingRequest, "id", 100L);
        ReflectiveHelper.setQuoteOwner(bookingRequest, 1L);
        bookingRequest.setAssignedAgent(agent(5L));
        Quote quote = pricedQuote(91L, bookingRequest, QuoteStatus.SENT);

        when(quoteRepository.findById(91L)).thenReturn(Optional.of(quote));
        when(quoteRepository.save(quote)).thenReturn(quote);

        var response = quoteService.rejectQuote(91L, auth0Subject);

        assertEquals(QuoteStatus.REJECTED, response.status());
        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        assertEquals("QUOTE_REJECTED", outboxCaptor.getValue().getEventType());
    }

    @Test
    void acceptQuoteRejectsQuoteOwnedByAnotherTraveler() {
        String auth0Subject = "auth0|traveler-1";
        Traveler traveler = new Traveler();
        ReflectionTestUtils.setField(traveler, "id", 1L);
        when(travelerRepository.findByAuth0Subject(auth0Subject)).thenReturn(Optional.of(traveler));

        Quote quote = new Quote();
        ReflectionTestUtils.setField(quote, "id", 90L);
        BookingRequest bookingRequest = new BookingRequest();
        ReflectiveHelper.setQuoteOwner(bookingRequest, 2L);
        quote.setBookingRequest(bookingRequest);
        quote.setStatus(QuoteStatus.SENT);
        quote.setCurrency("EUR");
        quote.setProviderPrice(BigDecimal.valueOf(670));
        quote.setAssistanceFee(BigDecimal.valueOf(100));
        quote.setTotalAmount(BigDecimal.valueOf(770));

        when(quoteRepository.findById(90L)).thenReturn(Optional.of(quote));

        assertThrows(ResourceNotFoundException.class, () -> quoteService.acceptQuote(90L, auth0Subject));
    }

    @Test
    void rejectQuoteRejectsQuoteOwnedByAnotherTraveler() {
        String auth0Subject = "auth0|traveler-1";
        Traveler traveler = new Traveler();
        ReflectionTestUtils.setField(traveler, "id", 1L);
        when(travelerRepository.findByAuth0Subject(auth0Subject)).thenReturn(Optional.of(traveler));

        Quote quote = new Quote();
        ReflectionTestUtils.setField(quote, "id", 91L);
        BookingRequest bookingRequest = new BookingRequest();
        ReflectiveHelper.setQuoteOwner(bookingRequest, 2L);
        quote.setBookingRequest(bookingRequest);
        quote.setStatus(QuoteStatus.SENT);
        quote.setCurrency("EUR");
        quote.setProviderPrice(BigDecimal.valueOf(670));
        quote.setAssistanceFee(BigDecimal.valueOf(100));
        quote.setTotalAmount(BigDecimal.valueOf(770));

        when(quoteRepository.findById(91L)).thenReturn(Optional.of(quote));

        assertThrows(ResourceNotFoundException.class, () -> quoteService.rejectQuote(91L, auth0Subject));
    }

    private static final class ReflectiveHelper {
        private static void setQuoteOwner(BookingRequest bookingRequest, Long travelerId) {
            Traveler traveler = new Traveler();
            ReflectionTestUtils.setField(traveler, "id", travelerId);
            com.odyssey.api.trip.Trip trip = new com.odyssey.api.trip.Trip();
            ReflectionTestUtils.setField(trip, "traveler", traveler);
            com.odyssey.api.need.Need need = new com.odyssey.api.need.Need();
            ReflectionTestUtils.setField(need, "trip", trip);
            bookingRequest.setNeed(need);
        }
    }

    private Quote quote(Long id, BookingRequest bookingRequest, QuoteStatus status) {
        Quote quote = new Quote();
        ReflectionTestUtils.setField(quote, "id", id);
        quote.setBookingRequest(bookingRequest);
        quote.setStatus(status);
        return quote;
    }

    private Quote pricedQuote(Long id, BookingRequest bookingRequest, QuoteStatus status) {
        Quote quote = quote(id, bookingRequest, status);
        quote.setProviderPrice(BigDecimal.valueOf(670));
        quote.setAssistanceFee(BigDecimal.valueOf(100));
        quote.setTotalAmount(BigDecimal.valueOf(770));
        quote.setCurrency("EUR");
        return quote;
    }

    private Agent agent(Long id) {
        Agent agent = new Agent();
        ReflectionTestUtils.setField(agent, "id", id);
        return agent;
    }
}
