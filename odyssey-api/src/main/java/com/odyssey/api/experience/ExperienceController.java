package com.odyssey.api.experience;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/experiences")
public class ExperienceController {

    private final ExperienceService experienceService;

    public ExperienceController(ExperienceService experienceService) {
        this.experienceService = experienceService;
    }

    @PostMapping
    public ExperienceResponse createExperience(
        @Valid @RequestBody CreateExperienceRequest request
    ) {
        return experienceService.createExperience(request);
    }

    @GetMapping
    public List<ExperienceResponse> getExperiences() {
        return experienceService.getExperiences();
    }

    @GetMapping("/{id}")
    public ExperienceResponse getExperience(@PathVariable Long id) {
        return experienceService.getExperience(id);
    }

    @PutMapping("/{id}")
    public ExperienceResponse updateExperience(
        @PathVariable Long id,
        @Valid @RequestBody CreateExperienceRequest request
    ) {
        return experienceService.updateExperience(id, request);
    }
}