package com.odyssey.api.quote;

import com.odyssey.api.agent.Agent;
import com.odyssey.api.booking.BookingRequest;
import com.odyssey.api.booking.BookingRequestRepository;
import com.odyssey.api.booking.BookingRequestStatus;
import com.odyssey.api.outbox.OutboxEventRepository;
import com.odyssey.api.payment.Payment;
import com.odyssey.api.payment.PaymentRepository;
import com.odyssey.api.payment.PaymentStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
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

    @Mock
    private QuoteRepository quoteRepository;

    @Mock
    private BookingRequestRepository bookingRequestRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private PaymentRepository paymentRepository;

    private QuoteService quoteService;

    @BeforeEach
    void setUp() {
        quoteService = new QuoteService(
            quoteRepository,
            bookingRequestRepository,
            outboxEventRepository,
            paymentRepository,
            new ObjectMapper()
        );
    }

    private BookingRequest inProgressBookingRequestAssignedTo(Long agentId) {
        Agent agent = new Agent();
        ReflectionTestUtils.setField(agent, "id", agentId);

        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setStatus(BookingRequestStatus.IN_PROGRESS);
        bookingRequest.setAssignedAgent(agent);
        return bookingRequest;
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
    void getQuotesByTravelerIncludesPaymentStatusFromLatestPayment() {

        Long travelerId = 5L;
        BookingRequest bookingRequest = new BookingRequest();
        ReflectionTestUtils.setField(bookingRequest, "id", 9L);

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
        when(paymentRepository.findFirstByQuoteIdOrderByCreatedAtDesc(42L))
            .thenReturn(Optional.of(payment));

        var responses = quoteService.getQuotesByTraveler(travelerId);

        assertEquals(1, responses.size());
        assertEquals(PaymentStatus.PAID, responses.get(0).paymentStatus());
    }

    @Test
    void getQuotesByTravelerReturnsNullPaymentStatusWhenNoPaymentExists() {

        Long travelerId = 5L;
        BookingRequest bookingRequest = new BookingRequest();

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
        when(paymentRepository.findFirstByQuoteIdOrderByCreatedAtDesc(43L))
            .thenReturn(Optional.empty());

        var responses = quoteService.getQuotesByTraveler(travelerId);

        assertEquals(1, responses.size());
        assertNull(responses.get(0).paymentStatus());
    }
}
