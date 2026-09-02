package com.odyssey.api.intent.recommendation;

public record AIIntentResult(
    String category,
    String activity,
    String destination
) {}