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

    public ClientEmailEventListener(
        TravelerRepository travelerRepository,
        AgentRepository agentRepository,
        QuoteRepository quoteRepository
    ) {
        this.travelerRepository = travelerRepository;
        this.agentRepository = agentRepository;
        this.quoteRepository = quoteRepository;
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

    private void handleBookingRequested(BookingRequestedEvent event) {

        Traveler traveler = travelerRepository
            .findById(event.travelerId())
            .orElseThrow(() ->
                new ResourceNotFoundException("Traveler not found")
            );

        System.out.println(
            "EMAIL CLIENT → " +
            traveler.getEmail() +
            " : Votre demande #" +
            event.bookingRequestId() +
            " a bien été reçue et va être traitée par un agent."
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

        System.out.println(
            "EMAIL CLIENT → " +
            traveler.getEmail() +
            " : Votre demande #" +
            event.bookingRequestId() +
            " est maintenant prise en charge par " +
            agent.getFirstName() +
            "."
        );
    }

    private void handleQuoteSent(QuoteSentEvent event) {

        Traveler traveler = travelerRepository
            .findById(event.travelerId())
            .orElseThrow();

        Quote quote = quoteRepository
            .findById(event.quoteId())
            .orElseThrow();

        System.out.println(
            "EMAIL CLIENT → " + traveler.getEmail()
            + " : Bonjour " + traveler.getFirstName()
            + ", une nouvelle proposition est disponible pour votre demande #"
            + event.bookingRequestId()
            + ". Prix : "
            + quote.getTotalAmount()
            + " "
            + quote.getCurrency()
            + ". " + quote.getDescription()
        );
    }
}
