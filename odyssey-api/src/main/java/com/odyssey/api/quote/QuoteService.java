package com.odyssey.api.quote;

import com.odyssey.api.agent.Agent;
import com.odyssey.api.agent.AgentRepository;
import com.odyssey.api.booking.BookingRequest;
import com.odyssey.api.booking.BookingRequestRepository;
import com.odyssey.api.booking.BookingRequestStatus;
import com.odyssey.api.booking.confirmation.Booking;
import com.odyssey.api.booking.confirmation.BookingRepository;
import com.odyssey.api.booking.confirmation.ProviderPaymentStatus;
import com.odyssey.api.event.QuoteSentEvent;
import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.outbox.OutboxEvent;
import com.odyssey.api.outbox.OutboxEventRepository;
import com.odyssey.api.outbox.OutboxStatus;
import com.odyssey.api.event.QuoteAcceptedEvent;
import com.odyssey.api.event.QuoteRejectedEvent;
import com.odyssey.api.event.TripQuotesSentEvent;
import com.odyssey.api.payment.Payment;
import com.odyssey.api.payment.PaymentRepository;
import com.odyssey.api.payment.PaymentStatus;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerRepository;
import com.odyssey.api.trip.SendTripQuotesResponse;
import com.odyssey.api.trip.Trip;
import com.odyssey.api.trip.TripRepository;
import com.odyssey.api.trip.TripStatus;

import tools.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class QuoteService {

    private final QuoteRepository quoteRepository;
        private final AgentRepository agentRepository;
    private final TravelerRepository travelerRepository;
    private final BookingRequestRepository bookingRequestRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;
    private final ObjectMapper objectMapper;

    public QuoteService(
            QuoteRepository quoteRepository,
            AgentRepository agentRepository,
            TravelerRepository travelerRepository,
            BookingRequestRepository bookingRequestRepository,
            OutboxEventRepository outboxEventRepository,
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            TripRepository tripRepository,
            ObjectMapper objectMapper
    ) {
        this.quoteRepository = quoteRepository;
                this.agentRepository = agentRepository;
        this.travelerRepository = travelerRepository;
        this.bookingRequestRepository = bookingRequestRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
                this.tripRepository = tripRepository;
        this.objectMapper = objectMapper;
    }

        @Transactional
        public QuoteResponse createQuote(
                        Long bookingRequestId,
                        String auth0Subject,
                        CreateQuoteRequest request
        ) {
                Agent agent = getCurrentAgent(auth0Subject);
                return createQuote(bookingRequestId, agent.getId(), request);
        }

    @Transactional
    public QuoteResponse createQuote(
            Long bookingRequestId,
            Long agentId,
            CreateQuoteRequest request
    ) {

        BookingRequest bookingRequest = bookingRequestRepository
                .findById(bookingRequestId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "BookingRequest not found"
                        )
                );

        // La demande doit avoir été prise en charge
        if (bookingRequest.getStatus() != BookingRequestStatus.IN_PROGRESS) {
            throw new IllegalArgumentException(
                    "BookingRequest must be IN_PROGRESS"
            );
        }

        // Un agent doit être assigné
        if (bookingRequest.getAssignedAgent() == null) {
            throw new IllegalArgumentException(
                    "BookingRequest has no assigned agent"
            );
        }

        // Et seul cet agent peut créer la proposition
        if (!bookingRequest.getAssignedAgent().getId().equals(agentId)) {
            throw new IllegalArgumentException(
                    "This BookingRequest is assigned to another agent"
            );
        }

        if (request.providerPrice() == null || request.providerPrice().signum() < 0) {
            throw new IllegalArgumentException(
                    "providerPrice must be zero or positive"
            );
        }

        if (request.assistanceFee() == null || request.assistanceFee().signum() < 0) {
            throw new IllegalArgumentException(
                    "assistanceFee must be zero or positive"
            );
        }

        Quote quote = new Quote();

        quote.setBookingRequest(bookingRequest);
        quote.setProvider(request.provider());
        quote.setExternalOfferId(request.externalOfferId());
        quote.setProviderPrice(request.providerPrice());
        quote.setAssistanceFee(request.assistanceFee());
        // totalAmount is ALWAYS computed server-side, never trusted from
        // the caller (Odyssey does not resell the travel service: the
        // traveler pays providerPrice + Odyssey's assistanceFee).
        quote.setTotalAmount(request.providerPrice().add(request.assistanceFee()));
        quote.setCurrency(request.currency());
        quote.setDescription(request.description());
        quote.setExpiresAt(request.expiresAt());

        quote.setStatus(QuoteStatus.DRAFT);
        quote.setCreatedAt(Instant.now());

        Quote savedQuote = quoteRepository.save(quote);

        return toResponse(savedQuote);
    }

    @Transactional
    public QuoteResponse sendQuote(
            Long quoteId,
            Long agentId
    ) {

        Quote quote = quoteRepository
                .findById(quoteId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Quote not found")
                );

        BookingRequest bookingRequest = quote.getBookingRequest();

        if (quote.getStatus() != QuoteStatus.DRAFT) {
            throw new IllegalArgumentException(
                    "Only a DRAFT quote can be sent"
            );
        }

        if (bookingRequest.getAssignedAgent() == null ||
                !bookingRequest.getAssignedAgent()
                        .getId()
                        .equals(agentId)) {

            throw new IllegalArgumentException(
                    "This BookingRequest is assigned to another agent"
            );
        }

        quote.setStatus(QuoteStatus.SENT);

        Quote savedQuote = quoteRepository.save(quote);

        Long travelerId = bookingRequest
                .getNeed()
                      .getTrip()
                      .getTraveler()
                      .getId();

              QuoteSentEvent event = new QuoteSentEvent(
                      savedQuote.getId(),
                      bookingRequest.getId(),
                      travelerId
              );

              String payload = objectMapper.writeValueAsString(event);

              OutboxEvent outboxEvent = new OutboxEvent();

              outboxEvent.setEventType("QUOTE_SENT");
              outboxEvent.setPayload(payload);
              outboxEvent.setStatus(OutboxStatus.PENDING);
              outboxEvent.setCreatedAt(Instant.now());

              outboxEventRepository.save(outboxEvent);

              return toResponse(savedQuote);
          }

    @Transactional
    public SendTripQuotesResponse sendDraftQuotesForTrip(
            Long tripId,
            String auth0Subject
    ) {
        Agent agent = getCurrentAgent(auth0Subject);
        Trip trip = tripRepository
                .findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        List<Quote> draftQuotes = quoteRepository
                .findByBookingRequestNeedTripIdAndStatusOrderByIdAsc(
                        tripId,
                        QuoteStatus.DRAFT
                );

        for (Quote quote : draftQuotes) {
            Agent assignedAgent = quote.getBookingRequest().getAssignedAgent();
            if (assignedAgent == null || !assignedAgent.getId().equals(agent.getId())) {
                throw new ResourceNotFoundException("Trip not found");
            }
        }

        if (draftQuotes.isEmpty()) {
            return new SendTripQuotesResponse(tripId, List.of());
        }

        draftQuotes.forEach(quote -> quote.setStatus(QuoteStatus.SENT));
        quoteRepository.saveAll(draftQuotes);

        List<Long> sentQuoteIds = draftQuotes.stream()
                .map(Quote::getId)
                .toList();
        TripQuotesSentEvent event = new TripQuotesSentEvent(
                tripId,
                trip.getTraveler().getId(),
                sentQuoteIds
        );
        saveOutboxEvent("TRIP_QUOTES_SENT", event);

        return new SendTripQuotesResponse(tripId, sentQuoteIds);
    }

    public List<TravelerQuoteResponse> getQuotesByTraveler(Long travelerId) {

        return quoteRepository
                .findByBookingRequestNeedTripTravelerIdAndStatusNot(
                        travelerId,
                        QuoteStatus.DRAFT
                )
                .stream()
                .map(this::toTravelerResponse)
                .toList();
    }

    public List<TravelerQuoteResponse> getQuotesByCurrentTraveler(String auth0Subject) {
        Traveler traveler = getCurrentTraveler(auth0Subject);
        return getQuotesByTraveler(traveler.getId());
    }

    private QuoteResponse toResponse(Quote quote) {

        return new QuoteResponse(
                quote.getId(),
                quote.getBookingRequest().getId(),
                quote.getProvider(),
                quote.getExternalOfferId(),
                quote.getProviderPrice(),
                quote.getAssistanceFee(),
                quote.getTotalAmount(),
                quote.getCurrency(),
                quote.getDescription(),
                quote.getStatus(),
                quote.getCreatedAt(),
                quote.getExpiresAt()
        );
    }

    @Transactional
    public TravelerQuoteResponse acceptQuote(
            Long quoteId,
            Long travelerId
    ) {

        Quote quote = quoteRepository
                .findById(quoteId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Quote not found")
                );

        BookingRequest bookingRequest = quote.getBookingRequest();

        Long quoteTravelerId = bookingRequest
                .getNeed()
                .getTrip()
                .getTraveler()
                .getId();

        if (!quoteTravelerId.equals(travelerId)) {
            throw new IllegalArgumentException(
                    "This quote does not belong to this traveler"
            );
        }

        if (quote.getStatus() != QuoteStatus.SENT) {
            throw new IllegalArgumentException(
                    "Only a SENT quote can be accepted"
            );
        }

        quote.setStatus(QuoteStatus.ACCEPTED);

        Quote savedQuote = quoteRepository.save(quote);

        updateTripConfirmationIfReady(bookingRequest.getNeed().getTrip());

        Long agentId = bookingRequest
                .getAssignedAgent()
                .getId();

        QuoteAcceptedEvent event = new QuoteAcceptedEvent(
                savedQuote.getId(),
                bookingRequest.getId(),
                travelerId,
                agentId
        );

        String payload = objectMapper.writeValueAsString(event);

        OutboxEvent outboxEvent = new OutboxEvent();

        outboxEvent.setEventType("QUOTE_ACCEPTED");
        outboxEvent.setPayload(payload);
        outboxEvent.setStatus(OutboxStatus.PENDING);
        outboxEvent.setCreatedAt(Instant.now());

        outboxEventRepository.save(outboxEvent);

        return toTravelerResponse(savedQuote);
    }

    @Transactional
    public TravelerQuoteResponse acceptQuote(Long quoteId, String auth0Subject) {
        Quote quote = getOwnedQuote(quoteId, auth0Subject);

        if (quote.getStatus() != QuoteStatus.SENT) {
            throw new IllegalArgumentException(
                    "Only a SENT quote can be accepted"
            );
        }

        quote.setStatus(QuoteStatus.ACCEPTED);

        Quote savedQuote = quoteRepository.save(quote);

        updateTripConfirmationIfReady(quote.getBookingRequest().getNeed().getTrip());

        Long travelerId = quote.getBookingRequest()
                .getNeed()
                .getTrip()
                .getTraveler()
                .getId();

        Long agentId = quote.getBookingRequest()
                .getAssignedAgent()
                .getId();

        QuoteAcceptedEvent event = new QuoteAcceptedEvent(
                savedQuote.getId(),
                quote.getBookingRequest().getId(),
                travelerId,
                agentId
        );

        String payload = objectMapper.writeValueAsString(event);

        OutboxEvent outboxEvent = new OutboxEvent();

        outboxEvent.setEventType("QUOTE_ACCEPTED");
        outboxEvent.setPayload(payload);
        outboxEvent.setStatus(OutboxStatus.PENDING);
        outboxEvent.setCreatedAt(Instant.now());

        outboxEventRepository.save(outboxEvent);

        return toTravelerResponse(savedQuote);
    }

    @Transactional
    public TravelerQuoteResponse rejectQuote(
            Long quoteId,
            Long travelerId
    ) {

        Quote quote = quoteRepository
                .findById(quoteId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Quote not found")
                );

        Long quoteTravelerId = quote
                .getBookingRequest()
                .getNeed()
                .getTrip()
                .getTraveler()
                .getId();

        // Sécurité : la proposition doit appartenir au Traveler
        if (!quoteTravelerId.equals(travelerId)) {
            throw new IllegalArgumentException(
                    "This quote does not belong to this traveler"
            );
        }

        // Seule une proposition envoyée peut être refusée
        if (quote.getStatus() != QuoteStatus.SENT) {
            throw new IllegalArgumentException(
                    "Only a SENT quote can be rejected"
            );
        }

        quote.setStatus(QuoteStatus.REJECTED);

        Quote savedQuote = quoteRepository.save(quote);

        Long agentId = quote.getBookingRequest().getAssignedAgent().getId();
        saveOutboxEvent(
                "QUOTE_REJECTED",
                new QuoteRejectedEvent(
                        savedQuote.getId(),
                        quote.getBookingRequest().getId(),
                        travelerId,
                        agentId
                )
        );

        return toTravelerResponse(savedQuote);
    }

    @Transactional
    public TravelerQuoteResponse rejectQuote(Long quoteId, String auth0Subject) {
        Quote quote = getOwnedQuote(quoteId, auth0Subject);

        if (quote.getStatus() != QuoteStatus.SENT) {
            throw new IllegalArgumentException(
                    "Only a SENT quote can be rejected"
            );
        }

        quote.setStatus(QuoteStatus.REJECTED);
        Quote savedQuote = quoteRepository.save(quote);

        Long travelerId = quote.getBookingRequest()
                .getNeed()
                .getTrip()
                .getTraveler()
                .getId();
        Long agentId = quote.getBookingRequest().getAssignedAgent().getId();
        saveOutboxEvent(
                "QUOTE_REJECTED",
                new QuoteRejectedEvent(
                        savedQuote.getId(),
                        quote.getBookingRequest().getId(),
                        travelerId,
                        agentId
                )
        );
        return toTravelerResponse(savedQuote);
    }

    /**
     * Single source of truth for deciding whether a Trip is ready to
     * become {@link TripStatus#CONFIRMED}, called by BOTH
     * {@code acceptQuote} overloads so the rule can never diverge between
     * them.
     *
     * <p>A Trip becomes CONFIRMED when every active (non-CANCELLED)
     * {@link BookingRequest} of the Trip has a "current" Quote (the Quote
     * with the highest id for that BookingRequest — see
     * {@link QuoteRepository#findFirstByBookingRequestIdOrderByIdDesc})
     * and that current Quote is {@link QuoteStatus#ACCEPTED}. Historical
     * Quotes (REJECTED, EXPIRED, or superseded by a newer one on the same
     * BookingRequest) are never inspected and can never block
     * confirmation — only the latest Quote per BookingRequest matters.</p>
     *
     * <p>Never downgrades an already CONFIRMED Trip.</p>
     */
    private void updateTripConfirmationIfReady(Trip trip) {
        if (trip.getStatus() == TripStatus.CONFIRMED) {
            return;
        }

        List<BookingRequest> activeBookingRequests = bookingRequestRepository
                .findByNeedTripId(trip.getId())
                .stream()
                .filter(bookingRequest -> bookingRequest.getStatus() != BookingRequestStatus.CANCELLED)
                .toList();

        boolean allActiveBookingRequestsHaveAnAcceptedCurrentQuote = !activeBookingRequests.isEmpty()
                && activeBookingRequests.stream().allMatch(this::hasAcceptedCurrentQuote);

        if (allActiveBookingRequestsHaveAnAcceptedCurrentQuote) {
            trip.setStatus(TripStatus.CONFIRMED);
        }
    }

    private boolean hasAcceptedCurrentQuote(BookingRequest bookingRequest) {
        return quoteRepository
                .findFirstByBookingRequestIdOrderByIdDesc(bookingRequest.getId())
                .map(currentQuote -> currentQuote.getStatus() == QuoteStatus.ACCEPTED)
                .orElse(false);
    }

    private Traveler getCurrentTraveler(String auth0Subject) {
        return travelerRepository
                .findByAuth0Subject(auth0Subject)
                .orElseThrow(() -> new ResourceNotFoundException("Traveler not found"));
    }

        private Agent getCurrentAgent(String auth0Subject) {
                return agentRepository
                                .findByAuth0Subject(auth0Subject)
                                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));
        }

        private void saveOutboxEvent(String eventType, Object event) {
                OutboxEvent outboxEvent = new OutboxEvent();
                outboxEvent.setEventType(eventType);
                outboxEvent.setPayload(objectMapper.writeValueAsString(event));
                outboxEvent.setStatus(OutboxStatus.PENDING);
                outboxEvent.setCreatedAt(Instant.now());
                outboxEventRepository.save(outboxEvent);
        }

    private Quote getOwnedQuote(Long quoteId, String auth0Subject) {
        Traveler traveler = getCurrentTraveler(auth0Subject);
        Quote quote = quoteRepository
                .findById(quoteId)
                .orElseThrow(() -> new ResourceNotFoundException("Quote not found"));

        Long quoteTravelerId = quote
                .getBookingRequest()
                .getNeed()
                .getTrip()
                .getTraveler()
                .getId();

        if (!quoteTravelerId.equals(traveler.getId())) {
            throw new ResourceNotFoundException("Quote not found");
        }

        return quote;
    }

    private TravelerQuoteResponse toTravelerResponse(Quote quote) {

        Long tripId = quote
                .getBookingRequest()
                .getNeed()
                .getTrip()
                .getId();

        PaymentStatus paymentStatus = paymentRepository
                .findFirstByTripIdOrderByCreatedAtDesc(tripId)
                .map(Payment::getStatus)
                .orElse(null);

        // A Booking may not exist yet. The Booking represents the selected
        // provider service, while the Trip Payment represents Odyssey's
        // assistance fee.
        Booking booking = bookingRepository
                .findByQuoteId(quote.getId())
                .orElse(null);

        return new TravelerQuoteResponse(
                quote.getId(),
                quote.getBookingRequest().getId(),
                quote.getProviderPrice(),
                quote.getAssistanceFee(),
                quote.getTotalAmount(),
                quote.getCurrency(),
                quote.getDescription(),
                quote.getStatus(),
                quote.getCreatedAt(),
                quote.getExpiresAt(),
                paymentStatus,
                booking != null ? booking.getProviderPaymentUrl() : null,
                booking != null ? booking.getProviderPaymentStatus() : ProviderPaymentStatus.NOT_REQUIRED_YET
        );
    }


        public List<AgentQuoteResponse> getQuotesByBookingRequest(
            Long bookingRequestId,
            String auth0Subject
    ) {
        Agent agent = agentRepository
                .findByAuth0Subject(auth0Subject)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));

        BookingRequest bookingRequest = bookingRequestRepository
                .findById(bookingRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking request not found"));

        if (bookingRequest.getAssignedAgent() == null
                || !bookingRequest.getAssignedAgent().getId().equals(agent.getId())) {
            throw new ResourceNotFoundException("Booking request not found");
        }

        return quoteRepository.findByBookingRequestIdOrderByIdDesc(bookingRequestId)
                .stream()
                .map(this::toAgentResponse)
                .toList();
    }

    private AgentQuoteResponse toAgentResponse(Quote quote) {
       Long tripId = quote
        .getBookingRequest()
        .getNeed()
        .getTrip()
        .getId();

        PaymentStatus paymentStatus = paymentRepository
        .findFirstByTripIdOrderByCreatedAtDesc(tripId)
        .map(Payment::getStatus)
        .orElse(null);

        Booking booking = bookingRepository
                .findByQuoteId(quote.getId())
                .orElse(null);

        return new AgentQuoteResponse(
                quote.getId(),
                quote.getBookingRequest().getId(),
                quote.getProvider(),
                quote.getExternalOfferId(),
                quote.getProviderPrice(),
                quote.getAssistanceFee(),
                quote.getTotalAmount(),
                quote.getCurrency(),
                quote.getDescription(),
                quote.getStatus(),
                quote.getCreatedAt(),
                quote.getExpiresAt(),
                paymentStatus,
                booking != null ? booking.getProviderPaymentUrl() : null,
                booking != null
                        ? booking.getProviderPaymentStatus()
                        : ProviderPaymentStatus.NOT_REQUIRED_YET
        );
    }
}