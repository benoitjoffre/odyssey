package com.odyssey.api.outbox;

import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.odyssey.api.event.KafkaEvent;
import tools.jackson.databind.ObjectMapper;

@Service
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(
        OutboxEventRepository outboxEventRepository,
        KafkaTemplate<String, Object> kafkaTemplate,
        ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {

        List<OutboxEvent> events =
            outboxEventRepository.findByStatus(OutboxStatus.PENDING);

        for (OutboxEvent event : events) {

            try {
              KafkaEvent kafkaEvent = new KafkaEvent(
                event.getEventType(),
                event.getPayload()
          );

              String message = objectMapper.writeValueAsString(kafkaEvent);

              kafkaTemplate.send(
                  "booking-events",
                  event.getId().toString(),
                  message
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