package com.odyssey.api.agent;

import java.time.Instant;

public record AgentNotificationResponse(
    Long id,
    String message,
    boolean read,
    Instant createdAt,
    Long bookingRequestId
) {}