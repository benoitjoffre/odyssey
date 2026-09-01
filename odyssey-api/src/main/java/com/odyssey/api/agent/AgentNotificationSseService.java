package com.odyssey.api.agent;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AgentNotificationSseService {

    private final Map<Long, SseEmitter> emitters =
        new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long agentId) {

        SseEmitter emitter =
            new SseEmitter(0L);

        emitters.put(agentId, emitter);

        emitter.onCompletion(() ->
            emitters.remove(agentId, emitter)
        );

        emitter.onTimeout(() ->
            emitters.remove(agentId, emitter)
        );

        emitter.onError(error ->
            emitters.remove(agentId, emitter)
        );

        return emitter;
    }

    public void send(
        Long agentId,
        AgentNotificationResponse notification
    ) {

        SseEmitter emitter = emitters.get(agentId);

        if (emitter == null) {
            return;
        }

        try {

            emitter.send(
                SseEmitter
                    .event()
                    .name("notification")
                    .data(notification)
            );

        } catch (IOException e) {

            emitters.remove(agentId, emitter);
        }
    }
}