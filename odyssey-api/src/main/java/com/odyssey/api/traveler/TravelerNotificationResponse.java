package com.odyssey.api.traveler;

import java.time.Instant;

public record TravelerNotificationResponse(
    Long id,
    String type,
    String title,
    String message,
    String targetUrl,
    boolean read,
    Instant createdAt
) {

    public static TravelerNotificationResponse from(TravelerNotification notification) {
        return new TravelerNotificationResponse(
            notification.getId(),
            notification.getType(),
            notification.getTitle(),
            notification.getMessage(),
            notification.getTargetUrl(),
            notification.isRead(),
            notification.getCreatedAt()
        );
    }
}
