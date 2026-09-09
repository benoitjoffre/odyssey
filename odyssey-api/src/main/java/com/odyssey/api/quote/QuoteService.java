package com.odyssey.api.quote;

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
import com.odyssey.api.payment.Payment;
import com.odyssey.api.payment.PaymentRepository;
import com.odyssey.api.payment.PaymentStatus;
import tools.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final BookingRequestRepository bookingRequestRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final ObjectMapper objectMapper;

    public QuoteService(
            QuoteRepository quoteRepository,
            BookingRequestRepository bookingRequestRepository,
            OutboxEventRepository outboxEventRepository,
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            ObjectMapper objectMapper
    ) {
        this.quoteRepository = quoteRepository;
        this.bookingRequestRepository = bookingRequestRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.objectMapper = objectMapper;
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

        return toTravelerResponse(savedQuote);
    }

    private TravelerQuoteResponse toTravelerResponse(Quote quote) {

        PaymentStatus paymentStatus = paymentRepository
                .findFirstByQuoteIdOrderByCreatedAtDesc(quote.getId())
                .map(Payment::getStatus)
                .orElse(null);

        // A Booking may not exist yet (e.g. before the Payment is PAID):
        // in that case there is nothing to tell the traveler yet about
        // paying the Provider directly.
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
}