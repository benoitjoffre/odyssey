package com.odyssey.api.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.odyssey.api.agent.Agent;
import com.odyssey.api.agent.AgentRepository;
import com.odyssey.api.agent.AgentStatus;

import tools.jackson.databind.ObjectMapper;

@Service
public class AgentEmailConsumer {

    private final ObjectMapper objectMapper;
    private final AgentRepository agentRepository;

    public AgentEmailConsumer(
        ObjectMapper objectMapper,
        AgentRepository agentRepository
    ) {
        this.objectMapper = objectMapper;
        this.agentRepository = agentRepository;
    }

    @KafkaListener(
        topics = "booking-events",
        groupId = "agent-emails"
    )
    public void consume(String message) {
        KafkaEvent kafkaEvent =
        objectMapper.readValue(
            message,
            KafkaEvent.class
        );

        if (!"BOOKING_REQUESTED".equals(kafkaEvent.type())) {
            return;
        }

        BookingRequestedEvent event =
            objectMapper.readValue(
            kafkaEvent.payload(),
            BookingRequestedEvent.class
        );

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