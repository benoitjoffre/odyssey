package com.odyssey.api.payment;

import com.odyssey.api.outbox.OutboxEvent;
import com.odyssey.api.outbox.OutboxEventRepository;
import com.odyssey.api.outbox.OutboxStatus;
import com.odyssey.api.payment.stripe.StripeCheckoutSession;
import com.odyssey.api.payment.stripe.StripeCheckoutSessionRequest;
import com.odyssey.api.payment.stripe.StripeGateway;
import com.odyssey.api.payment.stripe.StripeProperties;
import com.odyssey.api.payment.stripe.StripeWebhookEvent;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerRepository;
import com.odyssey.api.trip.Trip;
import com.odyssey.api.trip.TripRepository;
import com.odyssey.api.trip.TripStatus;

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
import static org.mockito.Mockito.mock;
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

    private static final Long TRIP_ID = 42L;
    private static final Long TRAVELER_ID = 5L;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private StripeGateway stripeGateway;

    private StripeProperties stripeProperties;
    private TravelerRepository travelerRepository;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        stripeProperties = new StripeProperties();
        stripeProperties.setSecretKey("sk_test_dummy");
        stripeProperties.setSuccessUrl("http://localhost:5173/traveler/quotes?payment=success");
        stripeProperties.setCancelUrl("http://localhost:5173/traveler/quotes?payment=cancelled");

        travelerRepository = mock(TravelerRepository.class);

        paymentService = new PaymentService(
            tripRepository,
            travelerRepository,
            paymentRepository,
            outboxEventRepository,
            stripeGateway,
            stripeProperties,
            new ObjectMapper()
        );
    }

    private Trip confirmedTrip() {
        Traveler traveler = new Traveler();
        ReflectionTestUtils.setField(traveler, "id", TRAVELER_ID);

        Trip trip = new Trip();
        ReflectionTestUtils.setField(trip, "id", TRIP_ID);
        ReflectionTestUtils.setField(trip, "traveler", traveler);
        trip.setStatus(TripStatus.CONFIRMED);
        trip.setAssistanceFee(BigDecimal.valueOf(100));
        return trip;
    }

    @Test
    void createCheckoutSessionCreatesPendingPaymentAndReturnsCheckoutUrl() {

        Trip trip = confirmedTrip();
        when(tripRepository.findByIdForUpdate(TRIP_ID)).thenReturn(Optional.of(trip));
        when(paymentRepository.findFirstByTripIdOrderByCreatedAtDesc(TRIP_ID))
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

        CheckoutSessionResponse response = paymentService.createCheckoutSession(TRIP_ID, TRAVELER_ID);

        assertEquals(100L, response.paymentId());
        assertEquals("https://checkout.stripe.com/test-session", response.checkoutUrl());

        ArgumentCaptor<StripeCheckoutSessionRequest> requestCaptor =
            ArgumentCaptor.forClass(StripeCheckoutSessionRequest.class);
        verify(stripeGateway).createCheckoutSession(requestCaptor.capture());
        // Odyssey only ever charges the assistance fee (100.00 EUR ->
        // 10000 cents), NEVER providerPrice + assistanceFee (770.00 EUR):
        // the Traveler pays the Provider directly, outside of Stripe.
        assertEquals(10000L, requestCaptor.getValue().amountInSmallestCurrencyUnit());
        assertEquals("eur", requestCaptor.getValue().currency());
        assertEquals("payment-100-attempt-1", requestCaptor.getValue().idempotencyKey());

        verify(paymentRepository, times(2)).save(any(Payment.class));
    }

    @Test
    void createCheckoutSessionRejectsNonConfirmedTrip() {

        Trip trip = confirmedTrip();
        trip.setStatus(TripStatus.DRAFT);
        when(tripRepository.findByIdForUpdate(TRIP_ID)).thenReturn(Optional.of(trip));

        assertThrows(
            IllegalArgumentException.class,
            () -> paymentService.createCheckoutSession(TRIP_ID, TRAVELER_ID)
        );
        verify(stripeGateway, never()).createCheckoutSession(any());
    }

    @Test
    void createCheckoutSessionRejectsAlreadyPaidTrip() {

        Trip trip = confirmedTrip();
        when(tripRepository.findByIdForUpdate(TRIP_ID)).thenReturn(Optional.of(trip));

        Payment paidPayment = new Payment();
        paidPayment.setStatus(PaymentStatus.PAID);
        when(paymentRepository.findFirstByTripIdOrderByCreatedAtDesc(TRIP_ID))
            .thenReturn(Optional.of(paidPayment));

        assertThrows(
            IllegalArgumentException.class,
            () -> paymentService.createCheckoutSession(TRIP_ID, TRAVELER_ID)
        );
        verify(stripeGateway, never()).createCheckoutSession(any());
    }

    @Test
    void createCheckoutSessionRejectsWhenTripBelongsToAnotherTraveler() {

        Trip trip = confirmedTrip();
        when(tripRepository.findByIdForUpdate(TRIP_ID)).thenReturn(Optional.of(trip));

        assertThrows(
            IllegalArgumentException.class,
            () -> paymentService.createCheckoutSession(TRIP_ID, 999L)
        );
        verify(stripeGateway, never()).createCheckoutSession(any());
    }

    @Test
    void createCheckoutSessionReusesExistingPendingPaymentRowOnDuplicateCheckout() {

        Trip trip = confirmedTrip();
        when(tripRepository.findByIdForUpdate(TRIP_ID)).thenReturn(Optional.of(trip));

        Payment existingPayment = new Payment();
        ReflectionTestUtils.setField(existingPayment, "id", 55L);
        existingPayment.setStatus(PaymentStatus.PENDING);
        existingPayment.setCheckoutAttempt(1);
        existingPayment.setStripeCheckoutSessionId("cs_test_456");
        when(paymentRepository.findFirstByTripIdOrderByCreatedAtDesc(TRIP_ID))
            .thenReturn(Optional.of(existingPayment));
        when(paymentRepository.save(any(Payment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(stripeGateway.createCheckoutSession(any(StripeCheckoutSessionRequest.class)))
            .thenReturn(new StripeCheckoutSession("cs_test_456", "https://checkout.stripe.com/test-session-2"));

        CheckoutSessionResponse response = paymentService.createCheckoutSession(TRIP_ID, TRAVELER_ID);

        assertEquals(55L, response.paymentId());
        // Still ONE Payment row: no new Payment was created, the existing
        // row (id 55) was reused and re-saved.
        ArgumentCaptor<Payment> savedPaymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(2)).save(savedPaymentCaptor.capture());
        for (Payment saved : savedPaymentCaptor.getAllValues()) {
            assertEquals(55L, saved.getId());
        }

        // Re-submitting while still PENDING must NOT bump the attempt
        // number: combined with the stable idempotency key, Stripe treats
        // this as the same request as the original attempt.
        ArgumentCaptor<StripeCheckoutSessionRequest> requestCaptor =
            ArgumentCaptor.forClass(StripeCheckoutSessionRequest.class);
        verify(stripeGateway).createCheckoutSession(requestCaptor.capture());
        assertEquals("payment-55-attempt-1", requestCaptor.getValue().idempotencyKey());
    }

    @Test
    void createCheckoutSessionUsesFreshAttemptAndIdempotencyKeyWhenRetryingAfterFailure() {

        Trip trip = confirmedTrip();
        when(tripRepository.findByIdForUpdate(TRIP_ID)).thenReturn(Optional.of(trip));

        Payment failedPayment = new Payment();
        ReflectionTestUtils.setField(failedPayment, "id", 77L);
        failedPayment.setStatus(PaymentStatus.FAILED);
        failedPayment.setCheckoutAttempt(1);
        failedPayment.setStripeCheckoutSessionId("cs_test_old_failed");
        when(paymentRepository.findFirstByTripIdOrderByCreatedAtDesc(TRIP_ID))
            .thenReturn(Optional.of(failedPayment));
        when(paymentRepository.save(any(Payment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(stripeGateway.createCheckoutSession(any(StripeCheckoutSessionRequest.class)))
            .thenReturn(new StripeCheckoutSession("cs_test_new", "https://checkout.stripe.com/test-session-retry"));

        CheckoutSessionResponse response = paymentService.createCheckoutSession(TRIP_ID, TRAVELER_ID);

        // Still ONE Payment row (id 77 reused), now back to PENDING with a
        // fresh Stripe session and a bumped attempt number so the
        // idempotency key differs from the FAILED attempt's old session.
        assertEquals(77L, response.paymentId());
        assertEquals(PaymentStatus.PENDING, failedPayment.getStatus());
        assertEquals(2, failedPayment.getCheckoutAttempt());

        ArgumentCaptor<StripeCheckoutSessionRequest> requestCaptor =
            ArgumentCaptor.forClass(StripeCheckoutSessionRequest.class);
        verify(stripeGateway).createCheckoutSession(requestCaptor.capture());
        assertEquals("payment-77-attempt-2", requestCaptor.getValue().idempotencyKey());
    }

    @Test
    void createCheckoutSessionMarksPaymentFailedAndRethrowsWhenStripeCallFails() {

        Trip trip = confirmedTrip();
        when(tripRepository.findByIdForUpdate(TRIP_ID)).thenReturn(Optional.of(trip));
        when(paymentRepository.findFirstByTripIdOrderByCreatedAtDesc(TRIP_ID))
            .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class)))
            .thenAnswer(invocation -> {
                Payment payment = invocation.getArgument(0);
                if (payment.getId() == null) {
                    ReflectionTestUtils.setField(payment, "id", 300L);
                }
                return payment;
            });
        when(stripeGateway.createCheckoutSession(any(StripeCheckoutSessionRequest.class)))
            .thenThrow(new IllegalStateException("Failed to create Stripe checkout session"));

        assertThrows(
            IllegalStateException.class,
            () -> paymentService.createCheckoutSession(TRIP_ID, TRAVELER_ID)
        );

        ArgumentCaptor<Payment> savedPaymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(2)).save(savedPaymentCaptor.capture());
        // The Payment row is left FAILED (not stuck PENDING with no
        // session), so a subsequent call is treated as a fresh retry.
        assertEquals(PaymentStatus.FAILED, savedPaymentCaptor.getAllValues().get(1).getStatus());
    }

    @Test
    void createCheckoutSessionTranslatesDuplicatePaymentConstraintViolationCleanly() {

        // Last line of defence: even though the Trip row lock should
        // already prevent this, a UNIQUE constraint violation on
        // payments.trip_id must never leak SQL/database details to the
        // caller.
        Trip trip = confirmedTrip();
        when(tripRepository.findByIdForUpdate(TRIP_ID)).thenReturn(Optional.of(trip));
        when(paymentRepository.findFirstByTripIdOrderByCreatedAtDesc(TRIP_ID))
            .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class)))
            .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate key value violates unique constraint"));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> paymentService.createCheckoutSession(TRIP_ID, TRAVELER_ID)
        );
        assertEquals("A payment for this trip is already being processed", exception.getMessage());
        verify(stripeGateway, never()).createCheckoutSession(any());
    }

    private Payment pendingPaymentFor(Trip trip, String checkoutSessionId) {
        Payment payment = new Payment();
        ReflectionTestUtils.setField(payment, "id", 200L);
        payment.setTrip(trip);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCurrency("EUR");
        payment.setProviderAmount(BigDecimal.ZERO);
        payment.setAssistanceFee(trip.getAssistanceFee());
        payment.setTotalAmount(trip.getAssistanceFee());
        payment.setStripeCheckoutSessionId(checkoutSessionId);
        return payment;
    }

    @Test
    void webhookMarksPaymentPaidAndCreatesPaymentSucceededOutboxEvent() {

        Trip trip = confirmedTrip();
        Payment payment = pendingPaymentFor(trip, "cs_test_123");

        when(paymentRepository.findByStripeCheckoutSessionId("cs_test_123"))
            .thenReturn(Optional.of(payment));
        when(paymentRepository.markAsPaidIfNotAlreadyPaid(
            eq(200L), any(), eq("pi_123")
        )).thenReturn(1);

        paymentService.processVerifiedEvent(
            new StripeWebhookEvent("evt_1", StripeWebhookEvent.CHECKOUT_SESSION_COMPLETED, "cs_test_123", "pi_123", 10000L, "eur")
        );

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());

        OutboxEvent savedEvent = outboxCaptor.getValue();
        assertEquals("PAYMENT_SUCCEEDED", savedEvent.getEventType());
        assertEquals(OutboxStatus.PENDING, savedEvent.getStatus());
    }

    @Test
    void webhookRefusesToMarkPaidWhenStripeAmountExceedsAssistanceFee() {

        // Payment.assistanceFee = 100 EUR (10000 cents). If Stripe ever
        // reports having charged 77000 cents (providerPrice + assistanceFee),
        // this MUST be rejected: Odyssey never collects the Provider's
        // money, so such an event cannot be trusted.
        Trip trip = confirmedTrip();
        Payment payment = pendingPaymentFor(trip, "cs_test_123");

        when(paymentRepository.findByStripeCheckoutSessionId("cs_test_123"))
            .thenReturn(Optional.of(payment));

        paymentService.processVerifiedEvent(
            new StripeWebhookEvent("evt_1", StripeWebhookEvent.CHECKOUT_SESSION_COMPLETED, "cs_test_123", "pi_123", 77000L, "eur")
        );

        verify(paymentRepository, never()).markAsPaidIfNotAlreadyPaid(any(), any(), any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void webhookRefusesToMarkPaidWhenStripeCurrencyMismatches() {

        Trip trip = confirmedTrip();
        Payment payment = pendingPaymentFor(trip, "cs_test_123");

        when(paymentRepository.findByStripeCheckoutSessionId("cs_test_123"))
            .thenReturn(Optional.of(payment));

        paymentService.processVerifiedEvent(
            new StripeWebhookEvent("evt_1", StripeWebhookEvent.CHECKOUT_SESSION_COMPLETED, "cs_test_123", "pi_123", 10000L, "usd")
        );

        verify(paymentRepository, never()).markAsPaidIfNotAlreadyPaid(any(), any(), any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void duplicateWebhookDeliveryDoesNotCreateDuplicateOutboxEvent() {

        Trip trip = confirmedTrip();
        Payment payment = pendingPaymentFor(trip, "cs_test_123");

        when(paymentRepository.findByStripeCheckoutSessionId("cs_test_123"))
            .thenReturn(Optional.of(payment));
        // The compare-and-swap update reports 0 rows changed: the payment
        // was already PAID by a previous delivery of this same webhook.
        when(paymentRepository.markAsPaidIfNotAlreadyPaid(
            eq(200L), any(), eq("pi_123")
        )).thenReturn(0);

        paymentService.processVerifiedEvent(
            new StripeWebhookEvent("evt_1", StripeWebhookEvent.CHECKOUT_SESSION_COMPLETED, "cs_test_123", "pi_123", 10000L, "eur")
        );

        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void webhookForUnknownCheckoutSessionIsIgnored() {

        when(paymentRepository.findByStripeCheckoutSessionId("cs_unknown"))
            .thenReturn(Optional.empty());

        paymentService.processVerifiedEvent(
            new StripeWebhookEvent("evt_1", StripeWebhookEvent.CHECKOUT_SESSION_COMPLETED, "cs_unknown", "pi_123", 10000L, "eur")
        );

        verify(paymentRepository, never()).markAsPaidIfNotAlreadyPaid(any(), any(), any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void nonCheckoutCompletedEventTypeIsIgnored() {

        paymentService.processVerifiedEvent(
            new StripeWebhookEvent("evt_1", "payment_intent.created", "cs_test_123", "pi_123", 10000L, "eur")
        );

        verify(paymentRepository, never()).findByStripeCheckoutSessionId(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void checkoutSessionExpiredMarksPendingPaymentFailed() {

        Trip trip = confirmedTrip();
        Payment payment = pendingPaymentFor(trip, "cs_test_123");

        when(paymentRepository.findByStripeCheckoutSessionId("cs_test_123"))
            .thenReturn(Optional.of(payment));

        paymentService.processVerifiedEvent(
            new StripeWebhookEvent("evt_2", StripeWebhookEvent.CHECKOUT_SESSION_EXPIRED, "cs_test_123", null, null, null)
        );

        verify(paymentRepository).markAsFailedIfPending(200L);
    }

    @Test
    void duplicateExpiredWebhookForAlreadyFailedPaymentIsIdempotent() {

        Trip trip = confirmedTrip();
        Payment payment = pendingPaymentFor(trip, "cs_test_123");
        payment.setStatus(PaymentStatus.FAILED);

        when(paymentRepository.findByStripeCheckoutSessionId("cs_test_123"))
            .thenReturn(Optional.of(payment));
        // The DB-level CAS (status = PENDING guard) no-ops: 0 rows updated
        // because the payment was already FAILED.
        when(paymentRepository.markAsFailedIfPending(200L)).thenReturn(0);

        paymentService.processVerifiedEvent(
            new StripeWebhookEvent("evt_3", StripeWebhookEvent.CHECKOUT_SESSION_EXPIRED, "cs_test_123", null, null, null)
        );

        verify(paymentRepository).markAsFailedIfPending(200L);
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
    }

    @Test
    void lateExpiredWebhookForAlreadyPaidPaymentDoesNotDowngradeIt() {

        Trip trip = confirmedTrip();
        Payment payment = pendingPaymentFor(trip, "cs_test_123");
        payment.setStatus(PaymentStatus.PAID);

        when(paymentRepository.findByStripeCheckoutSessionId("cs_test_123"))
            .thenReturn(Optional.of(payment));
        // The DB-level CAS (status = PENDING guard) is what actually
        // prevents the downgrade; the service always issues the call, but
        // it is a guaranteed no-op for a non-PENDING payment.
        when(paymentRepository.markAsFailedIfPending(200L)).thenReturn(0);

        paymentService.processVerifiedEvent(
            new StripeWebhookEvent("evt_4", StripeWebhookEvent.CHECKOUT_SESSION_EXPIRED, "cs_test_123", null, null, null)
        );

        verify(paymentRepository).markAsFailedIfPending(200L);
        assertEquals(PaymentStatus.PAID, payment.getStatus());
    }

    @Test
    void checkoutSessionAsyncPaymentFailedMarksPendingPaymentFailed() {

        Trip trip = confirmedTrip();
        Payment payment = pendingPaymentFor(trip, "cs_test_123");

        when(paymentRepository.findByStripeCheckoutSessionId("cs_test_123"))
            .thenReturn(Optional.of(payment));

        paymentService.processVerifiedEvent(
            new StripeWebhookEvent("evt_5", StripeWebhookEvent.CHECKOUT_SESSION_ASYNC_PAYMENT_FAILED, "cs_test_123", null, null, null)
        );

        verify(paymentRepository).markAsFailedIfPending(200L);
    }

    @Test
    void lateAsyncPaymentFailedWebhookForAlreadyPaidPaymentDoesNotDowngradeIt() {

        Trip trip = confirmedTrip();
        Payment payment = pendingPaymentFor(trip, "cs_test_123");
        payment.setStatus(PaymentStatus.PAID);

        when(paymentRepository.findByStripeCheckoutSessionId("cs_test_123"))
            .thenReturn(Optional.of(payment));
        when(paymentRepository.markAsFailedIfPending(200L)).thenReturn(0);

        paymentService.processVerifiedEvent(
            new StripeWebhookEvent("evt_6", StripeWebhookEvent.CHECKOUT_SESSION_ASYNC_PAYMENT_FAILED, "cs_test_123", null, null, null)
        );

        verify(paymentRepository).markAsFailedIfPending(200L);
        assertEquals(PaymentStatus.PAID, payment.getStatus());
    }

    @Test
    void expiredWebhookForSupersededOldAttemptSessionDoesNotAffectCurrentPendingAttempt() {

        // Attempt 1 used cs_attempt_1 and ended FAILED; the Traveler
        // retried, so the Payment row now points at cs_attempt_2 and is
        // PENDING again. cs_attempt_1 is no longer stored anywhere for
        // this Payment, so Stripe delivering a LATE
        // checkout.session.expired for cs_attempt_1 must be treated as an
        // unknown/superseded session and must NOT fail the new attempt.
        when(paymentRepository.findByStripeCheckoutSessionId("cs_attempt_1"))
            .thenReturn(Optional.empty());

        paymentService.processVerifiedEvent(
            new StripeWebhookEvent("evt_7", StripeWebhookEvent.CHECKOUT_SESSION_EXPIRED, "cs_attempt_1", null, null, null)
        );

        verify(paymentRepository, never()).markAsFailedIfPending(any());
    }

    @Test
    void completedWebhookForSupersededOldAttemptSessionDoesNotMarkCurrentAttemptPaid() {

        // Symmetric to the expiration case above: a late SUCCESS event for
        // an old, superseded checkout attempt must not mark the current
        // (different) attempt PAID.
        when(paymentRepository.findByStripeCheckoutSessionId("cs_attempt_1"))
            .thenReturn(Optional.empty());

        paymentService.processVerifiedEvent(
            new StripeWebhookEvent("evt_8", StripeWebhookEvent.CHECKOUT_SESSION_COMPLETED, "cs_attempt_1", "pi_old", 10000L, "eur")
        );

        verify(paymentRepository, never()).markAsPaidIfNotAlreadyPaid(any(), any(), any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void handleWebhookPayloadDoesNotMutateAnythingWhenSignatureIsInvalid() {

        when(stripeGateway.verifyAndParseEvent(any(), any()))
            .thenThrow(new IllegalArgumentException("Invalid Stripe webhook signature"));

        assertThrows(
            IllegalArgumentException.class,
            () -> paymentService.handleWebhookPayload("payload", "bad-signature")
        );

        verify(paymentRepository, never()).findByStripeCheckoutSessionId(any());
        verify(paymentRepository, never()).markAsPaidIfNotAlreadyPaid(any(), any(), any());
        verify(paymentRepository, never()).markAsFailedIfPending(any());
        verify(outboxEventRepository, never()).save(any());
    }
}
