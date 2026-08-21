package com.odyssey.api.agent;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateAgentRequest(

    @NotBlank
    String firstName,

    @NotBlank
    String lastName,

    @NotBlank
    @Email
    String email

) {}