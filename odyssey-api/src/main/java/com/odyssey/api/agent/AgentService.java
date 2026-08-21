package com.odyssey.api.agent;

import java.util.List;

import org.springframework.stereotype.Service;

import com.odyssey.api.exception.ResourceNotFoundException;

@Service
public class AgentService {

    private final AgentRepository agentRepository;
    private final AgentNotificationRepository notificationRepository;

    public AgentService(AgentRepository agentRepository, AgentNotificationRepository notificationRepository) {
        this.agentRepository = agentRepository;
        this.notificationRepository = notificationRepository;
    }

    public AgentResponse createAgent(CreateAgentRequest request) {

        Agent agent = new Agent();

        agent.setFirstName(request.firstName());
        agent.setLastName(request.lastName());
        agent.setEmail(request.email());
        agent.setStatus(AgentStatus.AVAILABLE);

        Agent savedAgent = agentRepository.save(agent);

        return toResponse(savedAgent);
    }

    public List<AgentResponse> getAgents() {
        return agentRepository.findAll()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public AgentResponse getAgent(Long id) {

        Agent agent = agentRepository
            .findById(id)
            .orElseThrow(() ->
                new ResourceNotFoundException("Agent not found")
            );

        return toResponse(agent);
    }

    private AgentResponse toResponse(Agent agent) {
        return new AgentResponse(
            agent.getId(),
            agent.getFirstName(),
            agent.getLastName(),
            agent.getEmail(),
            agent.getStatus()
        );
    }

    public List<AgentNotificationResponse> getNotifications(Long agentId) {

      if (!agentRepository.existsById(agentId)) {
          throw new ResourceNotFoundException("Agent not found");
      }

      return notificationRepository
          .findByAgentIdOrderByCreatedAtDesc(agentId)
          .stream()
          .map(notification ->
              new AgentNotificationResponse(
                  notification.getId(),
                  notification.getMessage(),
                  notification.isRead(),
                  notification.getCreatedAt(),
                  notification.getBookingRequest().getId()
              )
          )
          .toList();
    }
}