package com.odyssey.api.security;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class CurrentUserController {

    private static final Logger logger =
        LoggerFactory.getLogger(CurrentUserController.class);

    private static final String TRAVELER_ROLE = "TRAVELER";

    private final CurrentUserService currentUserService;

    public CurrentUserController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    } 

    @GetMapping("/me")
    public CurrentUserResponse getCurrentUser(
        @AuthenticationPrincipal Jwt jwt
    ) {
        String auth0Subject = jwt.getSubject();
        List<String> effectiveRoles = EffectiveRolesResolver.resolve(
            jwt.getClaimAsStringList("https://odyssey.app/roles")
        );

        currentUserService.provisionUser(jwt);

        Boolean onboardingCompleted = null;
        CurrentTravelerProfile travelerProfile = null;
        if (effectiveRoles.contains(TRAVELER_ROLE)) {
            onboardingCompleted = currentUserService
                .getTravelerOnboardingStatus(auth0Subject);

            travelerProfile = currentUserService.getCurrentTravelerProfile(auth0Subject);

            if (onboardingCompleted == null) {
                logger.warn(
                    "Traveler role is effective for subject {} but no traveler row was found; defaulting onboardingCompleted to false",
                    auth0Subject
                );
                onboardingCompleted = Boolean.FALSE;
            }
        }

        return new CurrentUserResponse(
            effectiveRoles,
            onboardingCompleted,
            travelerProfile != null ? travelerProfile.firstName() : null,
            travelerProfile != null ? travelerProfile.lastName() : null,
            travelerProfile != null ? travelerProfile.email() : null,
            travelerProfile != null ? travelerProfile.phoneNumber() : null,
            travelerProfile != null ? travelerProfile.whatsappNumber() : null,
            travelerProfile != null ? travelerProfile.preferredLanguage() : null

        );
    }

    @PreAuthorize("hasRole('TRAVELER')")
    @PatchMapping("/me")
    public CurrentUserResponse updateCurrentTravelerProfile(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        List<String> effectiveRoles = EffectiveRolesResolver.resolve(
            jwt.getClaimAsStringList("https://odyssey.app/roles")
        );

        currentUserService.provisionUser(jwt);
        CurrentTravelerProfile updatedProfile = currentUserService
            .updateCurrentTravelerProfile(jwt.getSubject(), request);

        return new CurrentUserResponse(
            effectiveRoles,
            currentUserService.getTravelerOnboardingStatus(jwt.getSubject()),
            updatedProfile.firstName(),
            updatedProfile.lastName(),
            updatedProfile.email(),
            updatedProfile.phoneNumber(),
            updatedProfile.whatsappNumber(),
            updatedProfile.preferredLanguage()
        );
    }
}
