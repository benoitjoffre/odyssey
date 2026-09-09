package com.odyssey.api.need.transfer;

public record TransferCriteriaResponse(
    String pickupLocation,
    String dropoffLocation,
    int travelers
) {}