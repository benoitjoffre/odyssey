package com.odyssey.api.agent;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AgentNotificationSseService {

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

        return emitter;
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
            return agentEmitters.isEmpty() ? null : agentEmitters;
        });
    }
}