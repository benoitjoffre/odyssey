package com.odyssey.api.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class BookingEventConsumer {

    @KafkaListener(
        topics = "booking-events",
        groupId = "agent-notifications"
    )
    public void consume(String message) {

        System.out.println(
            "KAFKA CONSUMER → " + message
        );
    }
}