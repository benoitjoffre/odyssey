package com.odyssey.api.agent;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AgentNotificationSseService {

    private static final Logger logger =
        LoggerFactory.getLogger(AgentNotificationSseService.class);

    private final Map<Long, Set<SseEmitter>> emitters =
        new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long agentId) {

        SseEmitter emitter =
            new SseEmitter(0L);

        emitters
            .computeIfAbsent(agentId, ignored -> ConcurrentHashMap.newKeySet())
            .add(emitter);

        emitter.onCompletion(() ->
            removeEmitter(agentId, emitter)
        );

        emitter.onTimeout(() ->
            removeEmitter(agentId, emitter)
        );

        emitter.onError(error ->
            removeEmitter(agentId, emitter)
        );

        try {
            emitter.send(SseEmitter.event().comment("connected"));
            logger.debug("Agent SSE connected for agent {}", agentId);
        } catch (IOException | IllegalStateException exception) {
            removeEmitter(agentId, emitter);
            emitter.completeWithError(exception);
        }

        return emitter;
    }

    @Scheduled(fixedRateString = "${odyssey.sse.heartbeat-ms:25000}")
    public void sendHeartbeats() {
        emitters.forEach((agentId, agentEmitters) -> {
            for (SseEmitter emitter : agentEmitters) {
                try {
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                } catch (IOException | IllegalStateException exception) {
                    removeEmitter(agentId, emitter);
                }
            }
        });
    }

    public void send(
        Long agentId,
        AgentNotificationResponse notification
    ) {

        Set<SseEmitter> agentEmitters = emitters.get(agentId);

        if (agentEmitters == null) {
            return;
        }

        for (SseEmitter emitter : agentEmitters) {
            try {
                emitter.send(
                    SseEmitter
                        .event()
                        .name("notification")
                        .data(notification)
                );
            } catch (IOException | IllegalStateException exception) {
                removeEmitter(agentId, emitter);
            }
        }
    }

    private void removeEmitter(Long agentId, SseEmitter emitter) {
        emitters.computeIfPresent(agentId, (ignored, agentEmitters) -> {
            agentEmitters.remove(emitter);
            logger.debug("Agent SSE disconnected for agent {}", agentId);
            return agentEmitters.isEmpty() ? null : agentEmitters;
        });
    }
}