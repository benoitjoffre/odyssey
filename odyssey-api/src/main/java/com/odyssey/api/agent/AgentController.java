package com.odyssey.api.agent;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
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
}