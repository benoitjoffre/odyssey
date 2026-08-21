package com.odyssey.api.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerRepository;

import tools.jackson.databind.ObjectMapper;

@Service
public class ClientEmailConsumer {

    private final ObjectMapper objectMapper;
    private final TravelerRepository travelerRepository;

    public ClientEmailConsumer(
        ObjectMapper objectMapper,
        TravelerRepository travelerRepository
    ) {
        this.objectMapper = objectMapper;
        this.travelerRepository = travelerRepository;
    }

    @KafkaListener(
        topics = "booking-events",
        groupId = "client-emails"
    )
    public void consume(String message) {

        BookingRequestedEvent event =
            objectMapper.readValue(
                message,
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
            " : Votre demande a bien été reçue " +
            "et va être traitée par un agent."
        );
    }
}