package com.odyssey.api.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class BookingEventProducer {

    private static final String TOPIC = "booking-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public BookingEventProducer(
        KafkaTemplate<String, Object> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishBookingRequested(
        BookingRequestedEvent event
    ) {
        kafkaTemplate.send(
            TOPIC,
            event.bookingRequestId().toString(),
            event
        );

        System.out.println(
            "KAFKA PRODUCER → BookingRequested " +
            event.bookingRequestId()
        );
    }
}