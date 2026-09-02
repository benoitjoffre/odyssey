package com.odyssey.api.intent.recommendation;

import com.odyssey.api.experience.ExperienceCategory;

public record ScoredExperienceResponse(
    Long id,
    String title,
    String description,
    ExperienceCategory category,
    String destination,
    Number durationDays,
    int score
) {}