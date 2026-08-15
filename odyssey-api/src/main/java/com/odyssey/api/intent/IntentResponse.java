package com.odyssey.api.intent;

import com.odyssey.api.experience.ExperienceCategory;

public record IntentResponse(
    Long id,
    String title,
    String description,
    IntentStatus status,
    ExperienceCategory category,
    Long travelerId
) {
}