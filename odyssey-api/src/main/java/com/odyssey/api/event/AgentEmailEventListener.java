package com.odyssey.api.event;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.odyssey.api.agent.Agent;
import com.odyssey.api.agent.AgentRepository;
import com.odyssey.api.agent.AgentStatus;
import com.odyssey.api.booking.BookingRequest;
import com.odyssey.api.booking.BookingRequestRepository;
import com.odyssey.api.exception.ResourceNotFoundException;

/**
 * Sends a (fake) email notification to an available agent when a new
 * booking request is created.
 *
 * <p>Replaces the previous {@code @KafkaListener}-based consumer (group
 * {@code agent-emails}): the reaction is unchanged, only the transport
 * (Spring application events instead of Kafka) differs.</p>
 */
@Component
public class AgentEmailEventListener {

    private static final Logger logger =
        LoggerFactory.getLogger(AgentEmailEventListener.class);

    private final AgentRepository agentRepository;
    private final BookingRequestRepository bookingRequestRepository;
    private final EmailService emailService;
    private final OdysseyEmailRenderer emailRenderer;

    public AgentEmailEventListener(
        AgentRepository agentRepository,
        BookingRequestRepository bookingRequestRepository,
        EmailService emailService,
        OdysseyEmailRenderer emailRenderer
    ) {
        this.agentRepository = agentRepository;
        this.bookingRequestRepository = bookingRequestRepository;
        this.emailService = emailService;
        this.emailRenderer = emailRenderer;
    }

    @EventListener
    public void onBookingRequested(BookingRequestedEvent event) {
        try {
            handleBookingRequested(event);
        } catch (Exception exception) {
            logger.error(
                "Failed to send agent email for booking request {}",
                event.bookingRequestId(),
                exception
            );
        }
    }

    @EventListener
    public void onQuoteAccepted(QuoteAcceptedEvent event) {
        try {
            handleQuoteAccepted(event);
        } catch (Exception exception) {
            logger.error(
                "Failed to send agent email for quote acceptance {}",
                event.bookingRequestId(),
                exception
            );
        }
    }

    @EventListener
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        try {
            handlePaymentSucceeded(event);
        } catch (Exception exception) {
            logger.error(
                "Failed to send agent payment email for trip {}",
                event.tripId(),
                exception
            );
        }
    }

    private void handleBookingRequested(BookingRequestedEvent event) {

        BookingRequest bookingRequest = bookingRequestRepository
            .findById(event.bookingRequestId())
            .orElseThrow(() ->
                new ResourceNotFoundException("Booking request not found")
            );

        Agent agent = agentRepository
            .findByStatus(AgentStatus.AVAILABLE)
            .stream()
            .findFirst()
            .orElseThrow(() ->
                new RuntimeException("No available agent")
            );

        String needLabel = switch (bookingRequest.getNeed().getType()) {
            case FLIGHT -> "un vol";
            case ACCOMMODATION -> "un hebergement";
            case TRANSFER -> "un transfert";
            case CAR -> "une voiture";
            case BUS -> "un bus";
        };

        emailService.sendEmail(
            emailRenderer.agentBookingRequested(
                agent.getEmail(),
                agent.getFirstName(),
                bookingRequest.getId(),
                bookingRequest.getNeed().getTrip().getTraveler().getFirstName(),
                bookingRequest.getNeed().getTrip().getTitle(),
                needLabel
            )
        );

        logger.info(
            "Agent email queued for booking request {} and agent {}",
            bookingRequest.getId(),
            agent.getId()
        );
    }

    private void handleQuoteAccepted(QuoteAcceptedEvent event) {

        BookingRequest bookingRequest = bookingRequestRepository
            .findById(event.bookingRequestId())
            .orElseThrow(() ->
                new ResourceNotFoundException("Booking request not found")
            );

        Agent agent = agentRepository
            .findById(event.agentId())
            .orElseThrow(() ->
                new ResourceNotFoundException("Agent not found")
            );

        emailService.sendEmail(
            emailRenderer.agentQuoteAccepted(
                agent.getEmail(),
                agent.getFirstName(),
                bookingRequest.getId(),
                bookingRequest.getNeed().getTrip().getTraveler().getFirstName()
            )
        );

        logger.info(
            "Agent email queued for quote acceptance on booking request {}",
            bookingRequest.getId()
        );
    }

    private void handlePaymentSucceeded(PaymentSucceededEvent event) {

        List<BookingRequest> bookingRequests = bookingRequestRepository
            .findByNeedTripId(event.tripId());

        Set<Long> alreadyNotified = new HashSet<>();

        for (BookingRequest bookingRequest : bookingRequests) {
            Agent agent = bookingRequest.getAssignedAgent();

            if (agent == null || alreadyNotified.contains(agent.getId())) {
                continue;
            }

            alreadyNotified.add(agent.getId());

            emailService.sendEmail(
                emailRenderer.agentPaymentSucceeded(
                    agent.getEmail(),
                    agent.getFirstName(),
                    event.tripId(),
                    event.assistanceFee(),
                    event.currency()
                )
            );
        }

        logger.info(
            "Agent payment email queued for trip {} ({} recipient(s))",
            event.tripId(),
            alreadyNotified.size()
        );
    }
}
