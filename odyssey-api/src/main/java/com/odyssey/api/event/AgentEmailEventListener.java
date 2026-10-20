package com.odyssey.api.event;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Component;
import com.odyssey.api.agent.Agent;
import com.odyssey.api.agent.AgentRepository;
import com.odyssey.api.agent.AgentStatus;
import com.odyssey.api.booking.BookingRequest;
import com.odyssey.api.booking.BookingRequestRepository;
import com.odyssey.api.exception.ResourceNotFoundException;

/** Prepares agent messages; delivery and retries are handled by the email queue. */
@Component
public class AgentEmailEventListener {

    private final AgentRepository agentRepository;
    private final BookingRequestRepository bookingRequestRepository;
    private final OdysseyEmailRenderer renderer;

    public AgentEmailEventListener(AgentRepository agentRepository,
        BookingRequestRepository bookingRequestRepository, OdysseyEmailRenderer renderer) {
        this.agentRepository = agentRepository;
        this.bookingRequestRepository = bookingRequestRepository;
        this.renderer = renderer;
    }

    public EmailMessage onBookingRequested(BookingRequestedEvent event) {
        BookingRequest request = request(event.bookingRequestId());
        Agent agent = request.getAssignedAgent();
        if (agent == null) {
            agent = agentRepository.findByStatus(AgentStatus.AVAILABLE).stream().findFirst()
                .orElseThrow(() -> new NoAvailableAgentException(request.getId()));
        }
        String needLabel = switch (request.getNeed().getType()) {
            case FLIGHT -> "un vol";
            case ACCOMMODATION -> "un hébergement";
            case TRANSFER -> "un transfert";
            case CAR -> "une voiture";
            case BUS -> "un bus";
        };
        return renderer.agentBookingRequested(agent.getEmail(), agent.getFirstName(), request.getId(),
            request.getNeed().getTrip().getTraveler().getFirstName(),
            request.getNeed().getTrip().getTitle(), needLabel);
    }

    public EmailMessage onQuoteAccepted(QuoteAcceptedEvent event) {
        Agent agent = agent(event.agentId());
        BookingRequest request = request(event.bookingRequestId());
        return renderer.agentQuoteAccepted(agent.getEmail(), agent.getFirstName(), request.getId(),
            request.getNeed().getTrip().getTraveler().getFirstName());
    }

    public EmailMessage onQuoteRejected(QuoteRejectedEvent event) {
        Agent agent = agent(event.agentId());
        BookingRequest request = request(event.bookingRequestId());
        return renderer.agentQuoteRejected(agent.getEmail(), agent.getFirstName(), request.getId(),
            request.getNeed().getTrip().getTraveler().getFirstName());
    }

    public List<EmailMessage> onPaymentSucceeded(PaymentSucceededEvent event) {
        var notified = new HashSet<Long>();
        var messages = new ArrayList<EmailMessage>();
        for (BookingRequest request : bookingRequestRepository.findByNeedTripId(event.tripId())) {
            Agent agent = request.getAssignedAgent();
            if (agent != null && notified.add(agent.getId())) {
                messages.add(renderer.agentPaymentSucceeded(agent.getEmail(), agent.getFirstName(),
                    event.tripId(), event.assistanceFee(), event.currency()));
            }
        }
        return messages;
    }

    private Agent agent(Long id) {
        return agentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));
    }

    private BookingRequest request(Long id) {
        return bookingRequestRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Booking request not found"));
    }

    private static final class NoAvailableAgentException extends IllegalStateException {
        private NoAvailableAgentException(Long requestId) {
            super("No available agent for booking request " + requestId);
        }
    }
}
