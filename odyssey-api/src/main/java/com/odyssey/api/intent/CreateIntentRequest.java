package com.odyssey.api.intent;

import com.odyssey.api.experience.ExperienceCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateIntentRequest(

    String title,

    @NotBlank
    String description,

    
    ExperienceCategory category,
    
    @NotNull
    Long travelerId

) {
}