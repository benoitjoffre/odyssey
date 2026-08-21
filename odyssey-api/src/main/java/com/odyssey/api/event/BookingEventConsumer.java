package com.odyssey.api.event;

import java.time.Instant;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.odyssey.api.agent.Agent;
import com.odyssey.api.agent.AgentNotification;
import com.odyssey.api.agent.AgentNotificationRepository;
import com.odyssey.api.agent.AgentRepository;
import com.odyssey.api.agent.AgentStatus;
import com.odyssey.api.booking.BookingRequest;
import com.odyssey.api.booking.BookingRequestRepository;
import com.odyssey.api.exception.ResourceNotFoundException;

import tools.jackson.databind.ObjectMapper;

@Service
public class BookingEventConsumer {

    private final ObjectMapper objectMapper;
    private final AgentRepository agentRepository;
    private final AgentNotificationRepository notificationRepository;
    private final BookingRequestRepository bookingRequestRepository;

    public BookingEventConsumer(
        ObjectMapper objectMapper,
        AgentRepository agentRepository,
        AgentNotificationRepository notificationRepository,
        BookingRequestRepository bookingRequestRepository
    ) {
        this.objectMapper = objectMapper;
        this.agentRepository = agentRepository;
        this.notificationRepository = notificationRepository;
        this.bookingRequestRepository = bookingRequestRepository;
    }

    @KafkaListener(
        topics = "booking-events",
        groupId = "agent-notifications"
    )
    public void consume(String message) {

        BookingRequestedEvent event =
            objectMapper.readValue(
                message,
                BookingRequestedEvent.class
            );

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
                new RuntimeException("No available agent")
            );

        AgentNotification notification =
            new AgentNotification();

        notification.setAgent(agent);
        notification.setBookingRequest(bookingRequest);
        notification.setMessage(
            "Nouvelle demande de réservation à prendre en charge"
        );
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());

        notificationRepository.save(notification);

        System.out.println(
            "NOTIFICATION → Agent " +
            agent.getId() +
            " / BookingRequest " +
            bookingRequest.getId()
        );
    }
}