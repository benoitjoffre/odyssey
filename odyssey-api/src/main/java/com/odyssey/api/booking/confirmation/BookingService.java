package com.odyssey.api.booking.confirmation;

import com.odyssey.api.agent.Agent;
import com.odyssey.api.agent.AgentRepository;
import com.odyssey.api.booking.BookingRequestStatus;
import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.quote.Quote;
import com.odyssey.api.quote.QuoteRepository;
import com.odyssey.api.quote.QuoteStatus;

import com.odyssey.api.event.BookingConfirmedEvent;
import com.odyssey.api.event.ProviderPaymentRequiredEvent;
import com.odyssey.api.outbox.OutboxEvent;
import com.odyssey.api.outbox.OutboxEventRepository;
import com.odyssey.api.outbox.OutboxStatus;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final QuoteRepository quoteRepository;
    private final FakeBookingProvider bookingProvider;
    private final AgentRepository agentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public BookingService(
        BookingRepository bookingRepository,
        QuoteRepository quoteRepository,
        FakeBookingProvider bookingProvider,
        AgentRepository agentRepository,
        OutboxEventRepository outboxEventRepository,
        ObjectMapper objectMapper
    ) {
        this.bookingRepository = bookingRepository;
        this.quoteRepository = quoteRepository;
        this.bookingProvider = bookingProvider;
        this.agentRepository = agentRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public BookingResponse createBooking(Long quoteId, String auth0Subject) {
        return createBooking(quoteId, getCurrentAgent(auth0Subject).getId());
    }

    @Transactional
    public BookingResponse createBooking(
        Long quoteId,
        Long agentId
    ) {

        Quote quote = quoteRepository
            .findById(quoteId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Quote not found"
                )
            );

        if (quote.getStatus() != QuoteStatus.ACCEPTED) {
            throw new IllegalArgumentException(
                "Only an ACCEPTED quote can be booked"
            );
        }

        if (quote.getBookingRequest().getAssignedAgent() == null ||
            !quote.getBookingRequest()
                .getAssignedAgent()
                .getId()
                .equals(agentId)) {

            throw new IllegalArgumentException(
                "This BookingRequest is assigned to another agent"
            );
        }

        if (bookingRepository.existsByQuoteId(quoteId)) {
            throw new IllegalArgumentException(
                "A booking already exists for this quote"
            );
        }

        Booking booking = new Booking();

        booking.setQuote(quote);
        booking.setStatus(BookingStatus.PENDING);
        booking.setCreatedAt(Instant.now());
        booking.setProviderPaymentStatus(ProviderPaymentStatus.NOT_REQUIRED_YET);

        Booking savedBooking =
            bookingRepository.save(booking);

        return toResponse(savedBooking);
    }

    private BookingResponse toResponse(Booking booking) {

        return new BookingResponse(
            booking.getId(),
            booking.getQuote().getId(),
            booking.getQuote()
                .getBookingRequest()
                .getId(),
            booking.getStatus(),
            booking.getProviderConfirmationId(),
            booking.getProviderReference(),
            booking.getProviderPaymentUrl(),
            booking.getProviderPaymentStatus(),
            booking.getCreatedAt(),
            booking.getConfirmedAt()
        );
    }

    /**
     * Lets the assigned Agent record what they know about the Traveler's
     * direct payment to the Provider (reference, external payment page,
     * status). Entirely independent from the Odyssey assistance
     * {@code Payment}: Odyssey never infers or verifies this itself.
     */
    @Transactional
    public BookingResponse updateProviderDetails(
        Long bookingId,
        String auth0Subject,
        String providerReference,
        String providerPaymentUrl,
        ProviderPaymentStatus providerPaymentStatus
    ) {
        return updateProviderDetails(
            bookingId,
            getCurrentAgent(auth0Subject).getId(),
            providerReference,
            providerPaymentUrl,
            providerPaymentStatus
        );
    }

    @Transactional
    public BookingResponse updateProviderDetails(
        Long bookingId,
        Long agentId,
        String providerReference,
        String providerPaymentUrl,
        ProviderPaymentStatus providerPaymentStatus
    ) {

        Booking booking = bookingRepository
            .findById(bookingId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Booking not found"
                )
            );

        var bookingRequest = booking.getQuote().getBookingRequest();

        if (bookingRequest.getAssignedAgent() == null ||
            !bookingRequest
                .getAssignedAgent()
                .getId()
                .equals(agentId)) {

            throw new IllegalArgumentException(
                "This BookingRequest is assigned to another agent"
            );
        }

        boolean paymentInstructionsChanged =
            !Objects.equals(booking.getProviderPaymentUrl(), providerPaymentUrl)
                || booking.getProviderPaymentStatus() != providerPaymentStatus;
        booking.setProviderReference(providerReference);
        booking.setProviderPaymentUrl(providerPaymentUrl);
        booking.setProviderPaymentStatus(
            providerPaymentStatus != null
                ? providerPaymentStatus
                : ProviderPaymentStatus.UNKNOWN
        );

        Booking savedBooking = bookingRepository.save(booking);

        if (paymentInstructionsChanged && providerPaymentUrl != null && !providerPaymentUrl.isBlank()
            && providerPaymentStatus == ProviderPaymentStatus.PAYMENT_REQUIRED) {
            var trip = bookingRequest.getNeed().getTrip();
            saveEvent("PROVIDER_PAYMENT_REQUIRED", new ProviderPaymentRequiredEvent(
                savedBooking.getId(), trip.getTraveler().getId(), trip.getId(), trip.getTitle()));
        }
        return toResponse(savedBooking);
    }

    @Transactional
    public BookingResponse confirmBooking(Long bookingId, String auth0Subject) {
        return confirmBooking(bookingId, getCurrentAgent(auth0Subject).getId());
    }

    @Transactional
    public BookingResponse confirmBooking(
        Long bookingId,
        Long agentId
    ) {

        Booking booking = bookingRepository
            .findById(bookingId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Booking not found"
                )
            );

        var bookingRequest =
            booking.getQuote().getBookingRequest();

        if (bookingRequest.getAssignedAgent() == null ||
            !bookingRequest
                .getAssignedAgent()
                .getId()
                .equals(agentId)) {

            throw new IllegalArgumentException(
                "This BookingRequest is assigned to another agent"
            );
        }

        if (!booking.canBeConfirmed()) {
            throw new IllegalStateException(
                "Booking cannot be confirmed"
            );
        }

        String confirmationId =
            bookingProvider.confirmBooking(booking);

        booking.setProviderConfirmationId(
            confirmationId
        );

        booking.confirm();
        
        bookingRequest.setStatus(
            BookingRequestStatus.COMPLETED
        );

        Booking savedBooking =
            bookingRepository.save(booking);

        var trip = bookingRequest.getNeed().getTrip();
        saveEvent("BOOKING_CONFIRMED", new BookingConfirmedEvent(
            savedBooking.getId(), trip.getTraveler().getId(), trip.getId(), trip.getTitle(),
            savedBooking.getProviderReference()));
        return toResponse(savedBooking);
    }

    private void saveEvent(String type, Object event) {
        OutboxEvent outbox = new OutboxEvent();
        outbox.setEventType(type);
        outbox.setPayload(objectMapper.writeValueAsString(event));
        outbox.setStatus(OutboxStatus.PENDING);
        outbox.setCreatedAt(Instant.now());
        outboxEventRepository.save(outbox);
    }

    private Agent getCurrentAgent(String auth0Subject) {
        return agentRepository
            .findByAuth0Subject(auth0Subject)
            .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));
    }

    public BookingResponse getBookingByQuote(Long quoteId, String auth0Subject) {
        var agent = getCurrentAgent(auth0Subject);

        var booking = bookingRepository
            .findByQuoteId(quoteId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getQuote().getBookingRequest().getAssignedAgent().getId().equals(agent.getId())) {
            throw new IllegalArgumentException("This BookingRequest is assigned to another agent");
        }

        return toResponse(booking);
    }
}