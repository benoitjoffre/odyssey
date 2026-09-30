package com.odyssey.api.destination;

public record DestinationResponse(
    Long id,
    String city,
    String country,
    String countryCode
) {
}
