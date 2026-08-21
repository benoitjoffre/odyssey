package com.odyssey.api.outbox;

import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    

    public OutboxPublisher(
        OutboxEventRepository outboxEventRepository,
        KafkaTemplate<String, Object> kafkaTemplate
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {

        List<OutboxEvent> events =
            outboxEventRepository.findByStatus(OutboxStatus.PENDING);

        for (OutboxEvent event : events) {

            try {
                kafkaTemplate.send(
                    "booking-events",
                    event.getId().toString(),
                    event.getPayload()
                ).get();

                event.setStatus(OutboxStatus.PUBLISHED);
                outboxEventRepository.save(event);

                System.out.println(
                    "OUTBOX → KAFKA : event " +
                    event.getId() +
                    " PUBLISHED"
                );

            } catch (Exception e) {

                System.err.println(
                    "OUTBOX → Kafka failed for event " +
                    event.getId()
                );
            }
        }
    }
}