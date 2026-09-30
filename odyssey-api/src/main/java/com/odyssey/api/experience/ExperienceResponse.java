package com.odyssey.api.experience;

import com.odyssey.api.destination.DestinationResponse;

public record ExperienceResponse(
    Long id,
    String title,
    String description,
    ExperienceCategory category,
    DestinationResponse destination,
    Number durationDays,
    String imageUrl
) {
}
