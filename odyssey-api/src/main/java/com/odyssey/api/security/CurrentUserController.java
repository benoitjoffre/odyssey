package com.odyssey.api.security;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        List<String> effectiveRoles = EffectiveRolesResolver.resolve(
            jwt.getClaimAsStringList("https://odyssey.app/roles")
        );

        currentUserService.provisionUser(jwt);

        Boolean onboardingCompleted = null;
        if (effectiveRoles.contains(TRAVELER_ROLE)) {
            onboardingCompleted = currentUserService
                .getTravelerOnboardingStatus(jwt.getSubject());

            if (onboardingCompleted == null) {
                logger.warn(
                    "Traveler role is effective for subject {} but no traveler row was found; defaulting onboardingCompleted to false",
                    jwt.getSubject()
                );
                onboardingCompleted = Boolean.FALSE;
            }
        }

        return new CurrentUserResponse(
            effectiveRoles,
            onboardingCompleted

        );
    }
}
