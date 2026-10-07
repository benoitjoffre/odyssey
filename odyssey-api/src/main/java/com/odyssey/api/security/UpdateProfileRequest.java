package com.odyssey.api.security;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
    @NotBlank
    String firstName,

    @NotBlank
    String lastName,

    @NotBlank
    String phoneNumber,

    String whatsappNumber,

    @NotBlank
    String preferredLanguage
) {
}