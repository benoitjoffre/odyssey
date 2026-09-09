package com.odyssey.api.payment;

import com.odyssey.api.booking.BookingRequest;
import com.odyssey.api.event.PaymentSucceededEvent;
import com.odyssey.api.exception.ResourceNotFoundException;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/**
 * Orchestrates the Traveler payment lifecycle: creating a Stripe Checkout
 * Session for an ACCEPTED Quote, and reconciling payment success from a
 * verified Stripe webhook.
 *
 * <p>The frontend never decides amounts or payment status: this service
 * always (re)reads the authoritative {@code providerPrice}/
 * {@code assistanceFee}/{@code totalAmount} from the persisted Quote.</p>
 */
@Service
public class PaymentService {

    private static final Logger logger =
        LoggerFactory.getLogger(PaymentService.class);

    private final QuoteRepository quoteRepository;
    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final StripeGateway stripeGateway;
    private final StripeProperties stripeProperties;
    private final ObjectMapper objectMapper;

    public PaymentService(
        QuoteRepository quoteRepository,
        PaymentRepository paymentRepository,
        OutboxEventRepository outboxEventRepository,
        StripeGateway stripeGateway,
        StripeProperties stripeProperties,
        ObjectMapper objectMapper
    ) {
        this.quoteRepository = quoteRepository;
        this.paymentRepository = paymentRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.stripeGateway = stripeGateway;
        this.stripeProperties = stripeProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * {@code noRollbackFor} lets a Stripe failure be recorded (Payment
     * moved to {@code FAILED}, see below) without discarding that write:
     * by default Spring would roll back the whole transaction — including
     * the FAILED status update — on any unchecked exception, which would
     * silently leave the Payment stuck PENDING forever with no Stripe
     * session, blocking any future retry.
     */
    @Transactional(noRollbackFor = IllegalStateException.class)
    public CheckoutSessionResponse createCheckoutSession(
        Long quoteId,
        Long travelerId
    ) {

        if (stripeProperties.getSecretKey() == null || stripeProperties.getSecretKey().isBlank()) {
            throw new IllegalStateException(
                "Stripe n'est pas configuré. Définissez STRIPE_SECRET_KEY côté backend."
            );
        }

        // Locks the Quote row (SELECT ... FOR UPDATE) for the rest of this
        // transaction. A second concurrent request for the same Quote
        // blocks here until this transaction commits or rolls back, so
        // only one request at a time can decide whether to create or
        // reuse this Quote's Payment: this is what prevents two concurrent
        // Checkout requests from ever creating two Payment rows for the
        // same Quote.
        Quote quote = quoteRepository
            .findByIdForUpdate(quoteId)
            .orElseThrow(() ->
                new ResourceNotFoundException("Quote not found")
            );

        Long quoteTravelerId = quote
            .getBookingRequest()
            .getNeed()
            .getTrip()
            .getTraveler()
            .getId();

        if (!quoteTravelerId.equals(travelerId)) {
            throw new IllegalArgumentException(
                "This quote does not belong to this traveler"
            );
        }

        if (quote.getStatus() != QuoteStatus.ACCEPTED) {
            throw new IllegalArgumentException(
                "Only an ACCEPTED quote can be paid"
            );
        }

        Optional<Payment> existingPayment =
            paymentRepository.findFirstByQuoteIdOrderByCreatedAtDesc(quoteId);

        if (existingPayment.isPresent()
            && existingPayment.get().getStatus() == PaymentStatus.PAID) {
            throw new IllegalArgumentException(
                "This quote has already been paid"
            );
        }

        // Odyssey's invariant is ONE Payment per Quote: reuse the existing
        // PENDING/FAILED payment row instead of creating a new one on
        // every checkout retry (also enforced by a UNIQUE constraint on
        // payments.quote_id as a last line of defence, see Payment.quote).
        boolean isNewPayment = existingPayment.isEmpty();
        boolean isRetryAfterFailure = existingPayment.isPresent()
            && existingPayment.get().getStatus() == PaymentStatus.FAILED;
        Payment payment = existingPayment.orElseGet(Payment::new);

        BigDecimal assistanceFee = resolveAssistanceFee(quote);
        BigDecimal totalAmount = resolveTotalAmount(quote, assistanceFee);

        payment.setQuote(quote);
        payment.setProviderAmount(quote.getProviderPrice());
        payment.setAssistanceFee(assistanceFee);
        payment.setTotalAmount(totalAmount);
        payment.setCurrency(quote.getCurrency());
        payment.setStatus(PaymentStatus.PENDING);
        if (payment.getCreatedAt() == null) {
            payment.setCreatedAt(Instant.now());
        }

        // Only bump the attempt number when a genuinely NEW Stripe session
        // is required (first-ever attempt, or retrying a FAILED payment).
        // Re-submitting while still PENDING keeps the same attempt number
        // on purpose: combined with the idempotency key below, Stripe then
        // recognizes it as the same request and returns the same Session
        // instead of creating a duplicate one.
        if (isNewPayment || isRetryAfterFailure) {
            int previousAttempt = payment.getCheckoutAttempt() != null
                ? payment.getCheckoutAttempt()
                : 0;
            payment.setCheckoutAttempt(previousAttempt + 1);
        } else if (payment.getCheckoutAttempt() == null) {
            payment.setCheckoutAttempt(1);
        }

        Payment savedPayment;
        try {
            savedPayment = paymentRepository.save(payment);
        } catch (DataIntegrityViolationException duplicatePayment) {
            // Last line of defence: the UNIQUE constraint on
            // payments.quote_id rejected a second Payment row for this
            // Quote. Should not normally happen (the Quote row lock above
            // already serializes this), but never leak SQL/database
            // details to the caller if it does.
            throw new IllegalArgumentException(
                "A payment for this quote is already being processed"
            );
        }

        String idempotencyKey = "payment-" + savedPayment.getId()
            + "-attempt-" + savedPayment.getCheckoutAttempt();

        StripeCheckoutSession session;
        try {
            // Odyssey is an assistance service: it never resells the travel
            // service and never collects the Provider's money. Stripe must
            // only ever charge the assistanceFee, never
            // providerAmount + assistanceFee (totalAmount). The Traveler pays
            // providerAmount directly to the Provider, outside of Stripe.
            session = stripeGateway.createCheckoutSession(
                new StripeCheckoutSessionRequest(
                    toSmallestCurrencyUnit(assistanceFee),
                    quote.getCurrency().toLowerCase(Locale.ROOT),
                    quote.getDescription(),
                    String.valueOf(quoteId),
                    String.valueOf(savedPayment.getId()),
                    stripeProperties.getSuccessUrl(),
                    stripeProperties.getCancelUrl(),
                    idempotencyKey
                )
            );
        } catch (IllegalStateException stripeFailure) {
            // Do not leave the Payment stuck PENDING with no Stripe
            // session: mark it FAILED so a subsequent call is correctly
            // treated as a fresh retry (see isRetryAfterFailure above).
            savedPayment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(savedPayment);
            throw stripeFailure;
        }

        savedPayment.setStripeCheckoutSessionId(session.id());
        paymentRepository.save(savedPayment);

        return new CheckoutSessionResponse(savedPayment.getId(), session.url());
    }

    @Transactional
    public void handleWebhookPayload(String payload, String signatureHeader) {

        StripeWebhookEvent event =
            stripeGateway.verifyAndParseEvent(payload, signatureHeader);

        processVerifiedEvent(event);
    }

    /**
     * Split out from {@link #handleWebhookPayload} so tests can exercise
     * the business reaction to an already-verified event without going
     * through real Stripe signature verification.
     *
     * <p>See {@link StripeWebhookEvent} for the list of handled event
     * types and why {@code payment_intent.payment_failed} is intentionally
     * not one of them.</p>
     */
    @Transactional
    void processVerifiedEvent(StripeWebhookEvent event) {

        if (event.checkoutSessionId() == null) {
            return;
        }

        if (event.isCheckoutSessionCompleted()) {
            handleCheckoutSessionCompleted(event);
        } else if (event.isTerminalFailure()) {
            handleCheckoutSessionTerminalFailure(event);
        }
    }

    private void handleCheckoutSessionCompleted(StripeWebhookEvent event) {

        Optional<Payment> paymentOptional = paymentRepository
            .findByStripeCheckoutSessionId(event.checkoutSessionId());

        if (paymentOptional.isEmpty()) {
            logger.warn(
                "Received Stripe webhook for unknown checkout session {}",
                event.checkoutSessionId()
            );
            return;
        }

        Payment payment = paymentOptional.get();

        // Attempt isolation (see handleCheckoutSessionTerminalFailure for
        // the full explanation): a late success event for a superseded
        // OLD checkout attempt must never mark the CURRENT attempt PAID.
        // findByStripeCheckoutSessionId already guarantees this since the
        // old session id is no longer stored anywhere once a new attempt
        // has replaced it, but this check makes the invariant explicit.
        if (!event.checkoutSessionId().equals(payment.getStripeCheckoutSessionId())) {
            logger.warn(
                "Ignoring Stripe {} webhook for checkout session {}: payment {} is now on a different checkout attempt",
                event.type(),
                event.checkoutSessionId(),
                payment.getId()
            );
            return;
        }

        // Reconcile Stripe's reported amount/currency against the ONLY
        // amount Odyssey ever collects: the assistance fee. Stripe must
        // never be trusted to mark a Payment PAID if what it actually
        // charged does not match what we asked it to charge (e.g. a
        // corrupted/forged event, or a Checkout Session created for a
        // different, unexpected amount).
        long expectedAmountInSmallestCurrencyUnit = toSmallestCurrencyUnit(payment.getAssistanceFee());
        String expectedCurrency = payment.getCurrency().toLowerCase(Locale.ROOT);

        boolean amountMatches = event.amountTotal() != null
            && event.amountTotal() == expectedAmountInSmallestCurrencyUnit;
        boolean currencyMatches = event.currency() != null
            && expectedCurrency.equalsIgnoreCase(event.currency());

        if (!amountMatches || !currencyMatches) {
            logger.error(
                "Refusing to mark payment {} PAID: Stripe reported {} {} but expected {} {}",
                payment.getId(),
                event.amountTotal(),
                event.currency(),
                expectedAmountInSmallestCurrencyUnit,
                expectedCurrency
            );
            return;
        }

        // Compare-and-swap: if this returns 0 the payment was already
        // PAID, meaning this is a duplicate webhook delivery that must be
        // safely ignored (no duplicate Outbox event / booking / agent
        // notification).
        int updated = paymentRepository.markAsPaidIfNotAlreadyPaid(
            payment.getId(),
            Instant.now(),
            event.paymentIntentId()
        );

        if (updated == 0) {
            return;
        }

        Quote quote = payment.getQuote();
        BookingRequest bookingRequest = quote.getBookingRequest();

        Long travelerId = bookingRequest
            .getNeed()
            .getTrip()
            .getTraveler()
            .getId();

        Long agentId = bookingRequest.getAssignedAgent() != null
            ? bookingRequest.getAssignedAgent().getId()
            : null;

        PaymentSucceededEvent domainEvent = new PaymentSucceededEvent(
            payment.getId(),
            quote.getId(),
            bookingRequest.getId(),
            travelerId,
            agentId,
            payment.getTotalAmount(),
            payment.getCurrency()
        );

        String outboxPayload = objectMapper.writeValueAsString(domainEvent);

        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setEventType("PAYMENT_SUCCEEDED");
        outboxEvent.setPayload(outboxPayload);
        outboxEvent.setStatus(OutboxStatus.PENDING);
        outboxEvent.setCreatedAt(Instant.now());

        outboxEventRepository.save(outboxEvent);
    }

    /**
     * Handles {@code checkout.session.expired} and
     * {@code checkout.session.async_payment_failed}: both are terminal
     * failure signals for a Checkout Session and must transition
     * {@code PENDING -> FAILED} only, never touching an already-PAID or
     * already-FAILED payment (see {@code PaymentRepository.markAsFailedIfPending}).
     *
     * <p><strong>Attempt isolation:</strong> {@code Payment.stripeCheckoutSessionId}
     * is overwritten with the newest Stripe Checkout Session id on every
     * retry ({@code createCheckoutSession}), so an OLD attempt's session id
     * is no longer stored anywhere once a new attempt has been created.
     * {@code findByStripeCheckoutSessionId} therefore naturally returns
     * empty for a late webhook belonging to a superseded attempt, which is
     * treated the same as an unknown session below. The extra equality
     * check against {@code payment.getStripeCheckoutSessionId()} makes this
     * invariant explicit so it can never silently break under a future
     * refactor of the lookup.</p>
     */
    private void handleCheckoutSessionTerminalFailure(StripeWebhookEvent event) {

        Optional<Payment> paymentOptional = paymentRepository
            .findByStripeCheckoutSessionId(event.checkoutSessionId());

        if (paymentOptional.isEmpty()) {
            logger.warn(
                "Received Stripe {} webhook for unknown or superseded checkout session {}",
                event.type(),
                event.checkoutSessionId()
            );
            return;
        }

        Payment payment = paymentOptional.get();

        if (!event.checkoutSessionId().equals(payment.getStripeCheckoutSessionId())) {
            logger.warn(
                "Ignoring Stripe {} webhook for checkout session {}: payment {} is now on a different checkout attempt",
                event.type(),
                event.checkoutSessionId(),
                payment.getId()
            );
            return;
        }

        // Compare-and-swap: 0 rows updated means the payment was not
        // PENDING (already PAID -> must stay PAID, or already FAILED ->
        // stays FAILED), which is the correct, idempotent no-op outcome.
        paymentRepository.markAsFailedIfPending(payment.getId());
    }

    /**
     * Converts a 2-decimal monetary amount (e.g. 770.00 EUR) into the
     * smallest currency unit Stripe expects (e.g. 77000 cents). Only
     * correct for 2-decimal currencies (EUR, USD, ...), which is all this
     * codebase currently deals with.
     */
    private long toSmallestCurrencyUnit(BigDecimal amount) {
        return amount
            .setScale(2, RoundingMode.HALF_UP)
            .movePointRight(2)
            .longValueExact();
    }

    private BigDecimal resolveAssistanceFee(Quote quote) {
        if (quote.getAssistanceFee() != null) {
            return quote.getAssistanceFee();
        }

        if (quote.getProviderPrice() == null || quote.getTotalAmount() == null) {
            throw new IllegalStateException(
                "Quote amounts are incomplete: providerPrice and totalAmount are required"
            );
        }

        BigDecimal computedAssistanceFee = quote.getTotalAmount().subtract(quote.getProviderPrice());
        if (computedAssistanceFee.signum() < 0) {
            throw new IllegalStateException(
                "Quote amounts are inconsistent: totalAmount is lower than providerPrice"
            );
        }

        logger.warn(
            "Quote {} has null assistanceFee; inferring it from totalAmount - providerPrice for backward compatibility",
            quote.getId()
        );
        return computedAssistanceFee;
    }

    private BigDecimal resolveTotalAmount(Quote quote, BigDecimal assistanceFee) {
        if (quote.getTotalAmount() != null) {
            return quote.getTotalAmount();
        }

        if (quote.getProviderPrice() == null) {
            throw new IllegalStateException(
                "Quote amounts are incomplete: providerPrice is required"
            );
        }

        return quote.getProviderPrice().add(assistanceFee);
    }
}
