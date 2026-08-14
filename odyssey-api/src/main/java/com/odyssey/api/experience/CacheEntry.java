package com.odyssey.api.experience;

import java.time.Instant;

public record CacheEntry(
    ExperienceResponse value,
    Instant expiresAt
) {
}