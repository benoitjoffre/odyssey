package com.odyssey.api.security;

import com.odyssey.api.traveler.Traveler;

public record CurrentTravelerProfile(
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    String whatsappNumber,
    String preferredLanguage
) {
    public static CurrentTravelerProfile from(Traveler traveler) {
        return new CurrentTravelerProfile(
            traveler.getFirstName(),
            traveler.getLastName(),
            traveler.getEmail(),
            traveler.getPhoneNumber(),
            traveler.getWhatsappNumber(),
            traveler.getPreferredLanguage()
        );
    }
}