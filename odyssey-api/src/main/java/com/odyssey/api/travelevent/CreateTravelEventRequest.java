package com.odyssey.api.travelevent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateTravelEventRequest(
    @NotBlank String name,
    @NotBlank String location,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    String description,
    @NotNull Long experienceId
) {}