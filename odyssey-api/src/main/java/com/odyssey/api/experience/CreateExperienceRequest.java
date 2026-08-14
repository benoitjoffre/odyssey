package com.odyssey.api.experience;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateExperienceRequest(
    @NotBlank
    String title,

    @NotBlank
    String description,

    @NotBlank
    String destination,

    @NotNull
    ExperienceCategory category,

    @NotNull
    Number durationDays
) {
}