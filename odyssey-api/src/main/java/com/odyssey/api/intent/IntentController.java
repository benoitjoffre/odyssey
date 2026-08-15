package com.odyssey.api.intent;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.odyssey.api.experience.ExperienceResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/intents")
public class IntentController {

    private final IntentService intentService;

    public IntentController(IntentService intentService) {
        this.intentService = intentService;
    }

    @PostMapping
    public IntentResponse createIntent(
        @Valid @RequestBody CreateIntentRequest request) {
        return intentService.createIntent(request);
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
    public List<ExperienceResponse> getRecommendations(@PathVariable Long id) {
        return intentService.getRecommendations(id);
    }
}