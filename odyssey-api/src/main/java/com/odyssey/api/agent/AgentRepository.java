package com.odyssey.api.agent;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentRepository extends JpaRepository<Agent, Long> {

    List<Agent> findByStatus(AgentStatus status);

    Optional<Agent> findByAuth0Subject(String auth0Subject);

    Optional<Agent> findByEmail(String email);
}