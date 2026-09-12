package com.odyssey.api.intent;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.odyssey.api.intent.recommendation.ScoredExperienceResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/intents")
public class IntentController {

    private final IntentService intentService;

    public IntentController(IntentService intentService) {
        this.intentService = intentService;
    }

    @PreAuthorize("hasRole('TRAVELER')")
    @PostMapping
    public IntentResponse createIntent(
        @Valid @RequestBody CreateIntentRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return intentService.createIntent(request, jwt.getSubject());
    }

    @GetMapping
    public List<IntentResponse> getIntents() {
        return intentService.getIntents();
    }

    @GetMapping("/{id}")
    public IntentResponse getIntent(@PathVariable Long id) {
        return intentService.getIntent(id);
    }

    @GetMapping("/{id}/recommendations")
    public List<ScoredExperienceResponse> getRecommendations(
        @PathVariable Long id
    ) {
        return intentService.getRecommendations(id);
    }
}