package com.odyssey.api.agent;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

    @PreAuthorize("hasRole('AGENT')")
    @GetMapping("/me/notifications")
    public List<AgentNotificationResponse> getNotifications(
        @AuthenticationPrincipal Jwt jwt
    ) {
        String auth0Subject = jwt.getSubject();
        return agentService.getNotifications(auth0Subject);
    }

    // @PreAuthorize("hasRole('AGENT')")
    // @GetMapping(
    //     value = "/me/notifications/stream",
    //     produces = "text/event-stream"
    // )
    // public SseEmitter streamNotifications(
    //     @AuthenticationPrincipal Jwt jwt
    // ) {
    //     String auth0Subject = jwt.getSubject();
    //     Agent agent = agentService.getAgentByAuth0Subject(auth0Subject);
    //     return sseService.subscribe(agent.getId());
    // }

    @PreAuthorize("hasRole('AGENT')")
@GetMapping(
    value = "/me/notifications/stream",
    produces = "text/event-stream"
)
public SseEmitter streamNotifications(
    @AuthenticationPrincipal Jwt jwt
) {
    Long agentId = agentService.getAgentIdByAuth0Subject(jwt.getSubject());

    return sseService.subscribe(agentId);
}
}