package com.odyssey.api.traveler;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/travelers")
public class TravelerController {

    private final TravelerService travelerService;
    private final TravelerNotificationSseService sseService;

    public TravelerController(
        TravelerService travelerService,
        TravelerNotificationSseService sseService
    ) {
        this.travelerService = travelerService;
        this.sseService = sseService;
    }

    @PreAuthorize("hasRole('AGENT')")
    @PostMapping
    public Traveler createTraveler(@Valid @RequestBody Traveler traveler) {
        return travelerService.createTraveler(traveler);
    }

    @PreAuthorize("hasRole('AGENT')")
    @GetMapping
    public List<Traveler> getTravelers() {
        return travelerService.getTravelers();
    }

    @PreAuthorize("hasRole('TRAVELER')")
    @GetMapping("/me/notifications")
    public List<TravelerNotificationResponse> getNotifications(
        @AuthenticationPrincipal Jwt jwt
    ) {
        String auth0Subject = jwt.getSubject();
        return travelerService.getNotifications(auth0Subject);
    }

    @PreAuthorize("hasRole('TRAVELER')")
    @GetMapping(value = "/me/notifications/stream", produces = "text/event-stream")
    public SseEmitter streamNotifications(@AuthenticationPrincipal Jwt jwt) {
        Traveler traveler = travelerService.getTravelerByAuth0Subject(jwt.getSubject());
        return sseService.subscribe(traveler.getId());
    }

    @PreAuthorize("hasRole('TRAVELER')")
    @PutMapping("/me/onboarding")
    public Traveler completeOnboarding(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TravelerOnboardingRequest request
    ) {
        return travelerService.completeOnboarding(jwt.getSubject(), request);
    }
}