package com.odyssey.api.event;

public record KafkaEvent(
    String type,
    String payload
) {}