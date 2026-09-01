package com.odyssey.api.booking.confirmation;

import com.odyssey.api.booking.BookingRequestStatus;
import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.quote.Quote;
import com.odyssey.api.quote.QuoteRepository;
import com.odyssey.api.quote.QuoteStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final QuoteRepository quoteRepository;
    private final FakeBookingProvider bookingProvider;

    public BookingService(
        BookingRepository bookingRepository,
        QuoteRepository quoteRepository,
        FakeBookingProvider bookingProvider
    ) {
        this.bookingRepository = bookingRepository;
        this.quoteRepository = quoteRepository;
        this.bookingProvider = bookingProvider;
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
            booking.getCreatedAt(),
            booking.getConfirmedAt()
        );
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

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException(
                "Only a PENDING booking can be confirmed"
            );
        }

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

        String confirmationId =
            bookingProvider.confirmBooking(booking);

        booking.setProviderConfirmationId(
            confirmationId
        );

        booking.setStatus(
            BookingStatus.CONFIRMED
        );

        booking.setConfirmedAt(
            Instant.now()
        );

        booking.setProviderConfirmationId(confirmationId);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(Instant.now());

        bookingRequest.setStatus(
            BookingRequestStatus.COMPLETED
        );

        Booking savedBooking =
            bookingRepository.save(booking);

        return toResponse(savedBooking);
    }
}