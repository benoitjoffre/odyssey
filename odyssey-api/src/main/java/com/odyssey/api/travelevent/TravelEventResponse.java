package com.odyssey.api.travelevent;

import java.time.LocalDate;

public record TravelEventResponse(
    Long id,
    String name,
    String location,
    LocalDate startDate,
    LocalDate endDate,
    String description,
    String imageUrl,
    Long experienceId
) {}