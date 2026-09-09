package com.odyssey.api.need;

import com.odyssey.api.need.transfer.TransferCriteriaResponse;

public record NeedResponse(
    Long id,
    NeedType type,
    NeedStatus status,
    String notes,
    Long tripId,
    TransferCriteriaResponse transferCriteria
) {}