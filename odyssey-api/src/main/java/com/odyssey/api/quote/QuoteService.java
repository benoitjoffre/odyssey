package com.odyssey.api.quote;

import com.odyssey.api.booking.BookingRequest;
import com.odyssey.api.booking.BookingRequestRepository;
import com.odyssey.api.booking.BookingRequestStatus;
import com.odyssey.api.event.QuoteSentEvent;
import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.outbox.OutboxEvent;
import com.odyssey.api.outbox.OutboxEventRepository;
import com.odyssey.api.outbox.OutboxStatus;

import tools.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final BookingRequestRepository bookingRequestRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public QuoteService(
            QuoteRepository quoteRepository,
            BookingRequestRepository bookingRequestRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.quoteRepository = quoteRepository;
        this.bookingRequestRepository = bookingRequestRepository;
        this.outboxEventRepository = outboxEventRepository;
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

        Quote quote = new Quote();

        quote.setBookingRequest(bookingRequest);
        quote.setProvider(request.provider());
        quote.setExternalOfferId(request.externalOfferId());
        quote.setProviderPrice(request.providerPrice());
        quote.setSellingPrice(request.sellingPrice());
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

    private QuoteResponse toResponse(Quote quote) {

        return new QuoteResponse(
                quote.getId(),
                quote.getBookingRequest().getId(),
                quote.getProvider(),
                quote.getExternalOfferId(),
                quote.getProviderPrice(),
                quote.getSellingPrice(),
                quote.getCurrency(),
                quote.getDescription(),
                quote.getStatus(),
                quote.getCreatedAt(),
                quote.getExpiresAt()
        );
    }
}