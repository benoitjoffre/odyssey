package com.odyssey.api.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.odyssey.api.agent.Agent;
import com.odyssey.api.agent.AgentRepository;
import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerRepository;

import tools.jackson.databind.ObjectMapper;

@Service
public class ClientEmailConsumer {

    private final ObjectMapper objectMapper;
    private final TravelerRepository travelerRepository;
    private final AgentRepository agentRepository;

    public ClientEmailConsumer(
        ObjectMapper objectMapper,
        TravelerRepository travelerRepository,
        AgentRepository agentRepository
    ) {
        this.objectMapper = objectMapper;
        this.travelerRepository = travelerRepository;
        this.agentRepository = agentRepository;
    }

    @KafkaListener(
    topics = "booking-events",
    groupId = "client-emails"
    )
    public void consume(String message) {

        KafkaEvent kafkaEvent =
            objectMapper.readValue(
                message,
                KafkaEvent.class
            );

        switch (kafkaEvent.type()) {

            case "BOOKING_REQUESTED" -> {
                handleBookingRequested(kafkaEvent.payload());
            }

            case "BOOKING_ASSIGNED" -> {
                handleBookingAssigned(kafkaEvent.payload());
            }

            default -> {
                System.out.println(
                    "CLIENT EMAIL → event ignored: " +
                    kafkaEvent.type()
                );
            }
        }
    }

    private void handleBookingRequested(String payload) {

    BookingRequestedEvent event =
        objectMapper.readValue(
            payload,
            BookingRequestedEvent.class
        );

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

    private void handleBookingAssigned(String payload) {

        BookingAssignedEvent event =
            objectMapper.readValue(
                payload,
                BookingAssignedEvent.class
            );

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
}