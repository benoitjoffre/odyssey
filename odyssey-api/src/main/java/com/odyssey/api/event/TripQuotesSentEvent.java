package com.odyssey.api.event;

import java.util.List;

public record TripQuotesSentEvent(
    Long tripId,
    Long travelerId,
    List<Long> quoteIds
) {}