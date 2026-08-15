package com.odyssey.api.intent;

import java.util.List;

import org.springframework.stereotype.Service;

import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.experience.ExperienceRepository;
import com.odyssey.api.experience.ExperienceResponse;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerRepository;

@Service
public class IntentService {

    private final IntentRepository intentRepository;
    private final TravelerRepository travelerRepository;
    private final ExperienceRepository experienceRepository;

    public IntentService(
        IntentRepository intentRepository,
        TravelerRepository travelerRepository,
        ExperienceRepository experienceRepository
    ) {
        this.intentRepository = intentRepository;
        this.travelerRepository = travelerRepository;
        this.experienceRepository = experienceRepository;
    }

    public IntentResponse createIntent(CreateIntentRequest request) {

        Traveler traveler = travelerRepository
            .findById(request.travelerId())
            .orElseThrow(() ->
                new ResourceNotFoundException("Traveler not found")
            );

        Intent intent = new Intent();
        intent.setTitle(request.title());
        intent.setDescription(request.description());
        intent.setStatus(IntentStatus.DRAFT);
        intent.setCategory(request.category());
        intent.setTraveler(traveler);

        Intent savedIntent = intentRepository.save(intent);

        return toResponse(savedIntent);
    }

    public IntentResponse getIntent(Long id) {
        Intent intent = intentRepository
            .findById(id)
            .orElseThrow(() ->
                new ResourceNotFoundException("Intent not found")
            );

        return toResponse(intent);
    }

    public List<IntentResponse> getIntents() {
        return intentRepository.findAll()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public List<ExperienceResponse> getRecommendations(Long intentId) {

        Intent intent = intentRepository
            .findById(intentId)
            .orElseThrow(() ->
                new ResourceNotFoundException("Intent not found")
            );

        return experienceRepository
            .findByCategory(intent.getCategory())
            .stream()
            .map(experience -> new ExperienceResponse(
                experience.getId(),
                experience.getTitle(),
                experience.getDescription(),
                experience.getCategory(),
                experience.getDestination(),
                experience.getDurationDays()
            ))
            .toList();
    }

    private IntentResponse toResponse(Intent intent) {
        return new IntentResponse(
            intent.getId(),
            intent.getTitle(),
            intent.getDescription(),
            intent.getStatus(),
            intent.getCategory(),
            intent.getTraveler().getId()
        );
    }
}