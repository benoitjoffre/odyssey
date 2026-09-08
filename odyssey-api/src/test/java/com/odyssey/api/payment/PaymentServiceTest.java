package com.odyssey.api.payment;

import com.odyssey.api.agent.Agent;
import com.odyssey.api.booking.BookingRequest;
import com.odyssey.api.need.Need;
import com.odyssey.api.outbox.OutboxEvent;
import com.odyssey.api.outbox.OutboxEventRepository;
import com.odyssey.api.outbox.OutboxStatus;
import com.odyssey.api.payment.stripe.StripeCheckoutSession;
import com.odyssey.api.payment.stripe.StripeCheckoutSessionRequest;
import com.odyssey.api.payment.stripe.StripeGateway;
import com.odyssey.api.payment.stripe.StripeProperties;
import com.odyssey.api.payment.stripe.StripeWebhookEvent;
import com.odyssey.api.quote.Quote;
import com.odyssey.api.quote.QuoteRepository;
import com.odyssey.api.quote.QuoteStatus;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.trip.Trip;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * No real Stripe network calls are made in these tests: {@link StripeGateway}
 * is a pure Mockito mock, so {@code Session.create}/{@code Webhook.constructEvent}
 * are never invoked.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final Long QUOTE_ID = 42L;
    private static final Long TRAVELER_ID = 5L;
    private static final Long AGENT_ID = 7L;
    private static final Long BOOKING_REQUEST_ID = 9L;

    @Mock
    private QuoteRepository quoteRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private StripeGateway stripeGateway;

    private StripeProperties stripeProperties;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        stripeProperties = new StripeProperties();
        stripeProperties.setSuccessUrl("http://localhost:5173/traveler/quotes?payment=success");
        stripeProperties.setCancelUrl("http://localhost:5173/traveler/quotes?payment=cancelled");

        paymentService = new PaymentService(
            quoteRepository,
            paymentRepository,
            outboxEventRepository,
            stripeGateway,
            stripeProperties,
            new ObjectMapper()
        );
    }

    private Quote acceptedQuote() {
        Traveler traveler = new Traveler();
        ReflectionTestUtils.setField(traveler, "id", TRAVELER_ID);

        Trip trip = new Trip();
        ReflectionTestUtils.setField(trip, "traveler", traveler);

        Need need = new Need();
        ReflectionTestUtils.setField(need, "trip", trip);

        Agent agent = new Agent();
        ReflectionTestUtils.setField(agent, "id", AGENT_ID);

        BookingRequest bookingRequest = new BookingRequest();
        ReflectionTestUtils.setField(bookingRequest, "id", BOOKING_REQUEST_ID);
        bookingRequest.setNeed(need);
        bookingRequest.setAssignedAgent(agent);

        Quote quote = new Quote();
        ReflectionTestUtils.setField(quote, "id", QUOTE_ID);
        quote.setBookingRequest(bookingRequest);
        quote.setStatus(QuoteStatus.ACCEPTED);
        quote.setCurrency("EUR");
        quote.setDescription("Hôtel test");
        quote.setProviderPrice(BigDecimal.valueOf(670));
        quote.setAssistanceFee(BigDecimal.valueOf(100));
        quote.setTotalAmount(BigDecimal.valueOf(770));
        return quote;
    }

    @Test
    void createCheckoutSessionCreatesPendingPaymentAndReturnsCheckoutUrl() {

        Quote quote = acceptedQuote();
        when(quoteRepository.findById(QUOTE_ID)).thenReturn(Optional.of(quote));
        when(paymentRepository.findFirstByQuoteIdOrderByCreatedAtDesc(QUOTE_ID))
            .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class)))
            .thenAnswer(invocation -> {
                Payment payment = invocation.getArgument(0);
                if (payment.getId() == null) {
                    ReflectionTestUtils.setField(payment, "id", 100L);
                }
                return payment;
            });
        when(stripeGateway.createCheckoutSession(any(StripeCheckoutSessionRequest.class)))
            .thenReturn(new StripeCheckoutSession("cs_test_123", "https://checkout.stripe.com/test-session"));

        CheckoutSessionResponse response = paymentService.createCheckoutSession(QUOTE_ID, TRAVELER_ID);

        assertEquals(100L, response.paymentId());
        assertEquals("https://checkout.stripe.com/test-session", response.checkoutUrl());

        ArgumentCaptor<StripeCheckoutSessionRequest> requestCaptor =
            ArgumentCaptor.forClass(StripeCheckoutSessionRequest.class);
        verify(stripeGateway).createCheckoutSession(requestCaptor.capture());
        // 770.00 EUR -> 77000 cents; the amount always comes from the
        // server-side Quote, never from the caller.
        assertEquals(77000L, requestCaptor.getValue().amountInSmallestCurrencyUnit());
        assertEquals("eur", requestCaptor.getValue().currency());

        verify(paymentRepository, times(2)).save(any(Payment.class));
    }

    @Test
    void createCheckoutSessionRejectsNonAcceptedQuote() {

        Quote quote = acceptedQuote();
        quote.setStatus(QuoteStatus.SENT);
        when(quoteRepository.findById(QUOTE_ID)).thenReturn(Optional.of(quote));

        assertThrows(
            IllegalArgumentException.class,
            () -> paymentService.createCheckoutSession(QUOTE_ID, TRAVELER_ID)
        );
        verify(stripeGateway, never()).createCheckoutSession(any());
    }

    @Test
    void createCheckoutSessionRejectsAlreadyPaidQuote() {

        Quote quote = acceptedQuote();
        when(quoteRepository.findById(QUOTE_ID)).thenReturn(Optional.of(quote));

        Payment paidPayment = new Payment();
        paidPayment.setStatus(PaymentStatus.PAID);
        when(paymentRepository.findFirstByQuoteIdOrderByCreatedAtDesc(QUOTE_ID))
            .thenReturn(Optional.of(paidPayment));

        assertThrows(
            IllegalArgumentException.class,
            () -> paymentService.createCheckoutSession(QUOTE_ID, TRAVELER_ID)
        );
        verify(stripeGateway, never()).createCheckoutSession(any());
    }

    @Test
    void createCheckoutSessionRejectsWhenQuoteBelongsToAnotherTraveler() {

        Quote quote = acceptedQuote();
        when(quoteRepository.findById(QUOTE_ID)).thenReturn(Optional.of(quote));

        assertThrows(
            IllegalArgumentException.class,
            () -> paymentService.createCheckoutSession(QUOTE_ID, 999L)
        );
        verify(stripeGateway, never()).createCheckoutSession(any());
    }

    @Test
    void createCheckoutSessionReusesExistingPendingPaymentRowOnDuplicateCheckout() {

        Quote quote = acceptedQuote();
        when(quoteRepository.findById(QUOTE_ID)).thenReturn(Optional.of(quote));

        Payment existingPayment = new Payment();
        ReflectionTestUtils.setField(existingPayment, "id", 55L);
        existingPayment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findFirstByQuoteIdOrderByCreatedAtDesc(QUOTE_ID))
            .thenReturn(Optional.of(existingPayment));
        when(paymentRepository.save(any(Payment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(stripeGateway.createCheckoutSession(any(StripeCheckoutSessionRequest.class)))
            .thenReturn(new StripeCheckoutSession("cs_test_456", "https://checkout.stripe.com/test-session-2"));

        CheckoutSessionResponse response = paymentService.createCheckoutSession(QUOTE_ID, TRAVELER_ID);

        assertEquals(55L, response.paymentId());

        ArgumentCaptor<Payment> savedPaymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(2)).save(savedPaymentCaptor.capture());
        for (Payment saved : savedPaymentCaptor.getAllValues()) {
            assertEquals(55L, saved.getId());
        }
    }

    private Payment pendingPaymentFor(Quote quote, String checkoutSessionId) {
        Payment payment = new Payment();
        ReflectionTestUtils.setField(payment, "id", 200L);
        payment.setQuote(quote);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCurrency(quote.getCurrency());
        payment.setProviderAmount(quote.getProviderPrice());
        payment.setAssistanceFee(quote.getAssistanceFee());
        payment.setTotalAmount(quote.getTotalAmount());
        payment.setStripeCheckoutSessionId(checkoutSessionId);
        return payment;
    }

    @Test
    void webhookMarksPaymentPaidAndCreatesPaymentSucceededOutboxEvent() {

        Quote quote = acceptedQuote();
        Payment payment = pendingPaymentFor(quote, "cs_test_123");

        when(paymentRepository.findByStripeCheckoutSessionId("cs_test_123"))
            .thenReturn(Optional.of(payment));
        when(paymentRepository.markAsPaidIfNotAlreadyPaid(
            eq(200L), any(), eq("pi_123")
        )).thenReturn(1);

        paymentService.processVerifiedEvent(
            new StripeWebhookEvent("evt_1", StripeWebhookEvent.CHECKOUT_SESSION_COMPLETED, "cs_test_123", "pi_123")
        );

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());

        OutboxEvent savedEvent = outboxCaptor.getValue();
        assertEquals("PAYMENT_SUCCEEDED", savedEvent.getEventType());
        assertEquals(OutboxStatus.PENDING, savedEvent.getStatus());
    }

    @Test
    void duplicateWebhookDeliveryDoesNotCreateDuplicateOutboxEvent() {

        Quote quote = acceptedQuote();
        Payment payment = pendingPaymentFor(quote, "cs_test_123");

        when(paymentRepository.findByStripeCheckoutSessionId("cs_test_123"))
            .thenReturn(Optional.of(payment));
        // The compare-and-swap update reports 0 rows changed: the payment
        // was already PAID by a previous delivery of this same webhook.
        when(paymentRepository.markAsPaidIfNotAlreadyPaid(
            eq(200L), any(), eq("pi_123")
        )).thenReturn(0);

        paymentService.processVerifiedEvent(
            new StripeWebhookEvent("evt_1", StripeWebhookEvent.CHECKOUT_SESSION_COMPLETED, "cs_test_123", "pi_123")
        );

        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void webhookForUnknownCheckoutSessionIsIgnored() {

        when(paymentRepository.findByStripeCheckoutSessionId("cs_unknown"))
            .thenReturn(Optional.empty());

        paymentService.processVerifiedEvent(
            new StripeWebhookEvent("evt_1", StripeWebhookEvent.CHECKOUT_SESSION_COMPLETED, "cs_unknown", "pi_123")
        );

        verify(paymentRepository, never()).markAsPaidIfNotAlreadyPaid(any(), any(), any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void nonCheckoutCompletedEventTypeIsIgnored() {

        paymentService.processVerifiedEvent(
            new StripeWebhookEvent("evt_1", "payment_intent.created", "cs_test_123", "pi_123")
        );

        verify(paymentRepository, never()).findByStripeCheckoutSessionId(any());
        verify(outboxEventRepository, never()).save(any());
    }
}
