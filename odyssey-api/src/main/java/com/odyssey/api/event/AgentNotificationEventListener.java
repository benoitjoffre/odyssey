package com.odyssey.api.event;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.odyssey.api.agent.Agent;
import com.odyssey.api.agent.AgentNotification;
import com.odyssey.api.agent.AgentNotificationRepository;
import com.odyssey.api.agent.AgentNotificationResponse;
import com.odyssey.api.agent.AgentNotificationSseService;
import com.odyssey.api.agent.AgentRepository;
import com.odyssey.api.agent.AgentStatus;
import com.odyssey.api.booking.BookingRequest;
import com.odyssey.api.booking.BookingRequestRepository;
import com.odyssey.api.exception.ResourceNotFoundException;

/**
 * Reacts to booking domain events by persisting agent notifications and
 * pushing them in real time over SSE.
 *
 * <p>Replaces the previous {@code @KafkaListener}-based consumer (group
 * {@code agent-notifications}): the reactions are unchanged, only the
 * transport (Spring application events instead of Kafka) differs.</p>
 */
@Component
public class AgentNotificationEventListener {

    private static final Logger logger =
        LoggerFactory.getLogger(AgentNotificationEventListener.class);

    private final AgentRepository agentRepository;
    private final AgentNotificationRepository notificationRepository;
    private final BookingRequestRepository bookingRequestRepository;
    private final AgentNotificationSseService sseService;

    public AgentNotificationEventListener(
        AgentRepository agentRepository,
        AgentNotificationRepository notificationRepository,
        BookingRequestRepository bookingRequestRepository,
        AgentNotificationSseService sseService
    ) {
        this.agentRepository = agentRepository;
        this.notificationRepository = notificationRepository;
        this.bookingRequestRepository = bookingRequestRepository;
        this.sseService = sseService;
    }

    @EventListener
    public void onBookingRequested(BookingRequestedEvent event) {
        try {
            handleBookingRequested(event);
        } catch (Exception exception) {
            logger.error(
                "Failed to handle BookingRequestedEvent for booking request {}",
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
                "Failed to handle QuoteAcceptedEvent for booking request {}",
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
                "Failed to handle PaymentSucceededEvent for booking request {}",
                event.bookingRequestId(),
                exception
            );
        }
    }

    private void handleBookingRequested(BookingRequestedEvent event) {

        BookingRequest bookingRequest =
            bookingRequestRepository
                .findById(event.bookingRequestId())
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Booking request not found"
                    )
                );

        Agent agent = agentRepository
            .findByStatus(AgentStatus.AVAILABLE)
            .stream()
            .findFirst()
            .orElseThrow(() ->
                new RuntimeException(
                    "No available agent"
                )
            );

        AgentNotification notification =
            new AgentNotification();

        notification.setAgent(agent);
        notification.setBookingRequest(
            bookingRequest
        );

        String travelerFirstName = bookingRequest
            .getNeed()
            .getTrip()
            .getTraveler()
            .getFirstName();
        String tripTitle = bookingRequest
            .getNeed()
            .getTrip()
            .getTitle();
        String needLabel = switch (bookingRequest.getNeed().getType()) {
            case FLIGHT -> "un vol";
            case ACCOMMODATION -> "un hébergement";
            case TRANSFER -> "un transfert";
            case CAR -> "une voiture";
            case BUS -> "un bus";
        };

        notification.setMessage(
            "Nouvelle demande · "
                + tripTitle
                + " — "
                + travelerFirstName
                + " a ajouté "
                + needLabel
                + " à son voyage."
        );

        notification.setRead(false);
        notification.setCreatedAt(Instant.now());

        AgentNotification savedNotification =
            notificationRepository.save(notification);

        AgentNotificationResponse response =
            AgentNotificationResponse.from(savedNotification);

        sseService.send(agent.getId(), response);

        System.out.println(
            "NOTIFICATION → Agent "
                + agent.getId()
                + " / BookingRequest "
                + bookingRequest.getId()
        );
    }

    private void handleQuoteAccepted(QuoteAcceptedEvent event) {

        BookingRequest bookingRequest =
            bookingRequestRepository
                .findById(event.bookingRequestId())
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Booking request not found"
                    )
                );

        Agent agent = agentRepository
            .findById(event.agentId())
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Agent not found"
                )
            );

        String travelerFirstName =
            bookingRequest
                .getNeed()
                .getTrip()
                .getTraveler()
                .getFirstName();

        AgentNotification notification =
            new AgentNotification();

        notification.setAgent(agent);
        notification.setBookingRequest(
            bookingRequest
        );

        notification.setMessage(
            travelerFirstName
                + " a accepté votre proposition pour la demande #"
                + bookingRequest.getId()
        );

        notification.setRead(false);
        notification.setCreatedAt(Instant.now());

        AgentNotification savedNotification =
            notificationRepository.save(notification);

        AgentNotificationResponse response =
            AgentNotificationResponse.from(savedNotification);

        sseService.send(
            agent.getId(),
            response
        );

        System.out.println(
            "NOTIFICATION → Agent "
                + agent.getId()
                + " : "
                + notification.getMessage()
        );
    }

    private void handlePaymentSucceeded(PaymentSucceededEvent event) {

        if (event.agentId() == null) {
            return;
        }

        BookingRequest bookingRequest =
            bookingRequestRepository
                .findById(event.bookingRequestId())
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Booking request not found"
                    )
                );

        Agent agent = agentRepository
            .findById(event.agentId())
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Agent not found"
                )
            );

        AgentNotification notification =
            new AgentNotification();

        notification.setAgent(agent);
        notification.setBookingRequest(bookingRequest);

        notification.setMessage(
            "Le client a payé la proposition ("
                + event.totalAmount()
                + " "
                + event.currency()
                + ") pour la demande #"
                + bookingRequest.getId()
                + " — vous pouvez procéder à la réservation."
        );

        notification.setRead(false);
        notification.setCreatedAt(Instant.now());

        AgentNotification savedNotification =
            notificationRepository.save(notification);

        AgentNotificationResponse response =
            AgentNotificationResponse.from(savedNotification);

        sseService.send(agent.getId(), response);

        System.out.println(
            "NOTIFICATION → Agent "
                + agent.getId()
                + " : "
                + notification.getMessage()
        );
    }
}
