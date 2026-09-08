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

    @Transactional
    public CheckoutSessionResponse createCheckoutSession(
        Long quoteId,
        Long travelerId
    ) {

        if (stripeProperties.getSecretKey() == null || stripeProperties.getSecretKey().isBlank()) {
            throw new IllegalStateException(
                "Stripe n'est pas configuré. Définissez STRIPE_SECRET_KEY côté backend."
            );
        }

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

        // Reuse the existing PENDING/FAILED payment row instead of
        // creating a new one on every checkout retry.
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

        Payment savedPayment = paymentRepository.save(payment);

        StripeCheckoutSession session = stripeGateway.createCheckoutSession(
            new StripeCheckoutSessionRequest(
                toSmallestCurrencyUnit(totalAmount),
                quote.getCurrency().toLowerCase(Locale.ROOT),
                quote.getDescription(),
                String.valueOf(quoteId),
                String.valueOf(savedPayment.getId()),
                stripeProperties.getSuccessUrl(),
                stripeProperties.getCancelUrl()
            )
        );

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
     */
    @Transactional
    void processVerifiedEvent(StripeWebhookEvent event) {

        if (!event.isCheckoutSessionCompleted() || event.checkoutSessionId() == null) {
            return;
        }

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
