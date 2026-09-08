package com.odyssey.api.booking.confirmation;

import com.odyssey.api.agent.Agent;
import com.odyssey.api.booking.BookingRequest;
import com.odyssey.api.payment.PaymentRepository;
import com.odyssey.api.payment.PaymentStatus;
import com.odyssey.api.quote.Quote;
import com.odyssey.api.quote.QuoteRepository;
import com.odyssey.api.quote.QuoteStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A Quote being ACCEPTED is not the same as the Traveler having paid:
 * {@link BookingService#createBooking} must never create a supplier
 * Booking before a PAID {@code Payment} exists for the quote.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final Long QUOTE_ID = 42L;
    private static final Long AGENT_ID = 7L;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private QuoteRepository quoteRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private FakeBookingProvider bookingProvider;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(
            bookingRepository,
            quoteRepository,
            paymentRepository,
            bookingProvider
        );
    }

    private Quote acceptedQuoteAssignedTo(Long agentId) {
        Agent agent = new Agent();
        ReflectionTestUtils.setField(agent, "id", agentId);

        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setAssignedAgent(agent);

        Quote quote = new Quote();
        ReflectionTestUtils.setField(quote, "id", QUOTE_ID);
        quote.setBookingRequest(bookingRequest);
        quote.setStatus(QuoteStatus.ACCEPTED);
        return quote;
    }

    @Test
    void createBookingThrowsWhenQuoteIsNotPaid() {

        Quote quote = acceptedQuoteAssignedTo(AGENT_ID);
        when(quoteRepository.findById(QUOTE_ID)).thenReturn(Optional.of(quote));
        when(paymentRepository.existsByQuoteIdAndStatus(QUOTE_ID, PaymentStatus.PAID))
            .thenReturn(false);

        assertThrows(
            IllegalArgumentException.class,
            () -> bookingService.createBooking(QUOTE_ID, AGENT_ID)
        );
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBookingSucceedsWhenQuoteIsPaid() {

        Quote quote = acceptedQuoteAssignedTo(AGENT_ID);
        when(quoteRepository.findById(QUOTE_ID)).thenReturn(Optional.of(quote));
        when(paymentRepository.existsByQuoteIdAndStatus(QUOTE_ID, PaymentStatus.PAID))
            .thenReturn(true);
        when(bookingRepository.existsByQuoteId(QUOTE_ID)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class)))
            .thenAnswer(invocation -> {
                Booking booking = invocation.getArgument(0);
                ReflectionTestUtils.setField(booking, "id", 500L);
                return booking;
            });

        BookingResponse response = bookingService.createBooking(QUOTE_ID, AGENT_ID);

        assertEquals(500L, response.id());
        assertEquals(BookingStatus.PENDING, response.status());
    }
}
