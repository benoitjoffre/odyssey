package com.odyssey.api.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.odyssey.api.agent.Agent;
import com.odyssey.api.agent.AgentRepository;
import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.quote.Quote;
import com.odyssey.api.quote.QuoteRepository;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerRepository;
import com.odyssey.api.trip.Trip;
import com.odyssey.api.trip.TripRepository;

/**
 * Sends (fake) email notifications to the traveler at each step of their
 * booking request: request received, agent assigned, quote available.
 *
 * <p>Replaces the previous {@code @KafkaListener}-based consumer (group
 * {@code client-emails}): the reactions are unchanged, only the transport
 * (Spring application events instead of Kafka) differs.</p>
 */
@Component
public class ClientEmailEventListener {

    private static final Logger logger =
        LoggerFactory.getLogger(ClientEmailEventListener.class);

    private final TravelerRepository travelerRepository;
    private final AgentRepository agentRepository;
    private final QuoteRepository quoteRepository;
    private final TripRepository tripRepository;
    private final EmailService emailService;
    private final OdysseyEmailRenderer emailRenderer;

    public ClientEmailEventListener(
        TravelerRepository travelerRepository,
        AgentRepository agentRepository,
        QuoteRepository quoteRepository,
        TripRepository tripRepository,
        EmailService emailService,
        OdysseyEmailRenderer emailRenderer
    ) {
        this.travelerRepository = travelerRepository;
        this.agentRepository = agentRepository;
        this.quoteRepository = quoteRepository;
        this.tripRepository = tripRepository;
        this.emailService = emailService;
        this.emailRenderer = emailRenderer;
    }

    @EventListener
    public void onBookingRequested(BookingRequestedEvent event) {
        try {
            handleBookingRequested(event);
        } catch (Exception exception) {
            logger.error(
                "Failed to send client email for booking request {}",
                event.bookingRequestId(),
                exception
            );
        }
    }

    @EventListener
    public void onBookingAssigned(BookingAssignedEvent event) {
        try {
            handleBookingAssigned(event);
        } catch (Exception exception) {
            logger.error(
                "Failed to send client email for booking assignment {}",
                event.bookingRequestId(),
                exception
            );
        }
    }

    @EventListener
    public void onQuoteSent(QuoteSentEvent event) {
        try {
            handleQuoteSent(event);
        } catch (Exception exception) {
            logger.error(
                "Failed to send client email for quote {}",
                event.quoteId(),
                exception
            );
        }
    }

    @EventListener
    public void onTripQuotesSent(TripQuotesSentEvent event) {
        try {
            handleTripQuotesSent(event);
        } catch (Exception exception) {
            logger.error(
                "Failed to send client email for trip {}",
                event.tripId(),
                exception
            );
        }
    }

    private void handleBookingRequested(BookingRequestedEvent event) {

        Traveler traveler = travelerRepository
            .findById(event.travelerId())
            .orElseThrow(() ->
                new ResourceNotFoundException("Traveler not found")
            );

        emailService.sendEmail(
            emailRenderer.travelerBookingRequested(
                traveler.getEmail(),
                traveler.getPreferredLanguage(),
                traveler.getFirstName(),
                event.bookingRequestId()
            )
        );

        logger.info(
            "Traveler email queued for booking request {}",
            event.bookingRequestId()
        );
    }

    private void handleBookingAssigned(BookingAssignedEvent event) {

        Traveler traveler = travelerRepository
            .findById(event.travelerId())
            .orElseThrow(() ->
                new ResourceNotFoundException("Traveler not found")
            );

        Agent agent = agentRepository
            .findById(event.agentId())
            .orElseThrow(() ->
                new ResourceNotFoundException("Agent not found")
            );

        emailService.sendEmail(
            emailRenderer.travelerBookingAssigned(
                traveler.getEmail(),
                traveler.getPreferredLanguage(),
                traveler.getFirstName(),
                event.bookingRequestId(),
                agent.getFirstName()
            )
        );

        logger.info(
            "Traveler email queued for booking assignment {}",
            event.bookingRequestId()
        );
    }

    private void handleQuoteSent(QuoteSentEvent event) {

        Traveler traveler = travelerRepository
            .findById(event.travelerId())
            .orElseThrow();

        Quote quote = quoteRepository
            .findById(event.quoteId())
            .orElseThrow();

        emailService.sendEmail(
            emailRenderer.travelerQuoteSent(
                traveler.getEmail(),
                traveler.getPreferredLanguage(),
                traveler.getFirstName(),
                event.bookingRequestId(),
                quote.getTotalAmount(),
                quote.getCurrency(),
                quote.getDescription()
            )
        );

        logger.info("Traveler email queued for quote {}", event.quoteId());
    }

    private void handleTripQuotesSent(TripQuotesSentEvent event) {
        Traveler traveler = travelerRepository
            .findById(event.travelerId())
            .orElseThrow(() -> new ResourceNotFoundException("Traveler not found"));
        Trip trip = tripRepository
            .findById(event.tripId())
            .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        logger.info(
            "Traveler email queued for trip {} after {} quote(s) were sent",
            event.tripId(),
            event.quoteIds().size()
        );
        emailService.sendEmail(
            emailRenderer.travelerTripQuotesSent(
                traveler.getEmail(),
                traveler.getPreferredLanguage(),
                traveler.getFirstName(),
                trip.getTitle(),
                event.quoteIds().size(),
                trip.getId()
            )
        );

        logger.info(
            "Traveler email sent for trip {} with {} quote(s)",
            trip.getId(),
            event.quoteIds().size()
        );
    }

    @EventListener
    public void onTravelerOnboardingCompleted(
        TravelerOnboardingCompletedEvent event
    ) {
        Traveler traveler = travelerRepository
            .findById(event.travelerId())
            .orElseThrow(() -> new ResourceNotFoundException("Traveler not found"));

        emailService.sendEmail(
            emailRenderer.travelerOnboardingCompleted(
                traveler.getEmail(),
                traveler.getPreferredLanguage(),
                traveler.getFirstName()
            )
        );

        logger.info(
            "Traveler email sent for onboarding completion {}",
            event.travelerId()
        );
    }
}
