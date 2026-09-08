package com.odyssey.api.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.odyssey.api.agent.Agent;
import com.odyssey.api.agent.AgentRepository;
import com.odyssey.api.agent.AgentStatus;

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

    public AgentEmailEventListener(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
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

    private void handleBookingRequested(BookingRequestedEvent event) {

        Agent agent = agentRepository
            .findByStatus(AgentStatus.AVAILABLE)
            .stream()
            .findFirst()
            .orElseThrow(() ->
                new RuntimeException("No available agent")
            );

        System.out.println(
            "EMAIL AGENT → " +
            agent.getEmail() +
            " : Bonjour " +
            agent.getFirstName() +
            ", une nouvelle demande de réservation #" +
            event.bookingRequestId() +
            " est disponible."
        );
    }
}
