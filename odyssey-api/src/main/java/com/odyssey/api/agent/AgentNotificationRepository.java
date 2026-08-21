package com.odyssey.api.agent;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentNotificationRepository
    extends JpaRepository<AgentNotification, Long> {

    List<AgentNotification> findByAgentIdOrderByCreatedAtDesc(Long agentId);
}