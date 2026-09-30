package com.odyssey.api.intent.recommendation;

import com.odyssey.api.destination.DestinationResponse;
import com.odyssey.api.experience.ExperienceCategory;

public record ScoredExperienceResponse(
    Long id,
    String title,
    String description,
    ExperienceCategory category,
    DestinationResponse destination,
    Number durationDays,
    int score
) {}