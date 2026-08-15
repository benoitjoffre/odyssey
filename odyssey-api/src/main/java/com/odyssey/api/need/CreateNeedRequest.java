package com.odyssey.api.need;

import jakarta.validation.constraints.NotNull;

public record CreateNeedRequest(

    @NotNull
    NeedType type,

    String notes,

    @NotNull
    Long tripId

) {}