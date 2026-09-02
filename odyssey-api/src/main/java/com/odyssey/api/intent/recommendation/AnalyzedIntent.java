package com.odyssey.api.intent.recommendation;

import com.odyssey.api.experience.ExperienceCategory;

public record AnalyzedIntent(
    ExperienceCategory category,
    String activity,
    String destination
) {}