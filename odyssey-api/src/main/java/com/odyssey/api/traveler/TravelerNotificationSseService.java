package com.odyssey.api.traveler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class TravelerNotificationSseService {

    private static final Logger logger =
        LoggerFactory.getLogger(TravelerNotificationSseService.class);

    private final Map<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long travelerId) {
        SseEmitter emitter = new SseEmitter(0L);

        emitters
            .computeIfAbsent(travelerId, ignored -> ConcurrentHashMap.newKeySet())
            .add(emitter);

        emitter.onCompletion(() -> removeEmitter(travelerId, emitter));
        emitter.onTimeout(() -> removeEmitter(travelerId, emitter));
        emitter.onError(error -> removeEmitter(travelerId, emitter));

        try {
            emitter.send(SseEmitter.event().comment("connected"));
            logger.debug("Traveler SSE connected for traveler {}", travelerId);
        } catch (IOException | IllegalStateException exception) {
            removeEmitter(travelerId, emitter);
            emitter.completeWithError(exception);
        }

        return emitter;
    }

    @Scheduled(fixedRateString = "${odyssey.sse.heartbeat-ms:25000}")
    public void sendHeartbeats() {
        emitters.forEach((travelerId, travelerEmitters) -> {
            for (SseEmitter emitter : travelerEmitters) {
                try {
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                } catch (IOException | IllegalStateException exception) {
                    removeEmitter(travelerId, emitter);
                }
            }
        });
    }

    public void send(Long travelerId, TravelerNotificationResponse notification) {
        Set<SseEmitter> travelerEmitters = emitters.get(travelerId);

        if (travelerEmitters == null) {
            return;
        }

        for (SseEmitter emitter : travelerEmitters) {
            try {
                emitter.send(
                    SseEmitter
                        .event()
                        .name("notification")
                        .data(notification)
                );
            } catch (IOException | IllegalStateException exception) {
                removeEmitter(travelerId, emitter);
            }
        }
    }

    private void removeEmitter(Long travelerId, SseEmitter emitter) {
        emitters.computeIfPresent(travelerId, (ignored, travelerEmitters) -> {
            travelerEmitters.remove(emitter);
            logger.debug("Traveler SSE disconnected for traveler {}", travelerId);
            return travelerEmitters.isEmpty() ? null : travelerEmitters;
        });
    }
}
