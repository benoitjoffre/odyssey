package com.odyssey.api.experience;

public record ExperienceResponse(
    Long id,
    String title,
    String description,
    ExperienceCategory category,
    String destination,
    Number durationDays
) {
}
