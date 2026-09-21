package com.odyssey.api.traveler;
import jakarta.validation.constraints.NotBlank;

public record TravelerOnboardingRequest(

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
