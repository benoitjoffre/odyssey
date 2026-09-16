package com.odyssey.api.trip;

import java.util.List;

public record SendTripQuotesResponse(
    Long tripId,
    List<Long> sentQuoteIds
) {}