package com.odyssey.api.agent;

public record AgentResponse(
    Long id,
    String firstName,
    String lastName,
    String email,
    AgentStatus status
) {}