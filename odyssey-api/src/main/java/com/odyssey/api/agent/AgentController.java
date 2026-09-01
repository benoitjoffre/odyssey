package com.odyssey.api.agent;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentService agentService;
    private final AgentNotificationSseService sseService;

    public AgentController(
        AgentService agentService,
        AgentNotificationSseService sseService
    ) {
        this.agentService = agentService;
        this.sseService = sseService;
    }

    @PostMapping
    public AgentResponse createAgent(
        @Valid @RequestBody CreateAgentRequest request
    ) {
        return agentService.createAgent(request);
    }

    @GetMapping
    public List<AgentResponse> getAgents() {
        return agentService.getAgents();
    }

    @GetMapping("/{id}")
    public AgentResponse getAgent(@PathVariable Long id) {
        return agentService.getAgent(id);
    }

    @GetMapping("/{id}/notifications")
    public List<AgentNotificationResponse> getNotifications(
        @PathVariable Long id
    ) {
        return agentService.getNotifications(id);
    }

    @GetMapping(
    value = "/{agentId}/notifications/stream",
    produces = "text/event-stream"
    )
    public SseEmitter streamNotifications(
        @PathVariable Long agentId
    ) {
        return sseService.subscribe(agentId);
    }
}