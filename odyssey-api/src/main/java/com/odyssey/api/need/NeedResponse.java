package com.odyssey.api.need;

public record NeedResponse(
    Long id,
    NeedType type,
    NeedStatus status,
    String notes,
    Long tripId
) {}