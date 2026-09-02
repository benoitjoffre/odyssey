package com.odyssey.api.intent;

import java.util.List;

import org.springframework.stereotype.Service;

import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.experience.ExperienceCategory;
import com.odyssey.api.experience.ExperienceRepository;
import com.odyssey.api.intent.recommendation.AnalyzedIntent;
import com.odyssey.api.intent.recommendation.IntentAnalysisService;
import com.odyssey.api.intent.recommendation.IntentAnalyzer;
import com.odyssey.api.intent.recommendation.RecommendationScorer;
import com.odyssey.api.intent.recommendation.ScoredExperienceResponse;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerRepository;

@Service
public class IntentService {

    private final IntentRepository intentRepository;
    private final TravelerRepository travelerRepository;
    private final ExperienceRepository experienceRepository;
    private final IntentAnalyzer intentAnalyzer;
    private final IntentAnalysisService intentAnalysisService;
    private final RecommendationScorer recommendationScorer;

    public IntentService(
        IntentRepository intentRepository,
        TravelerRepository travelerRepository,
        ExperienceRepository experienceRepository,
        IntentAnalyzer intentAnalyzer,
        IntentAnalysisService intentAnalysisService,
        RecommendationScorer recommendationScorer
    ) {
        this.intentRepository = intentRepository;
        this.travelerRepository = travelerRepository;
        this.experienceRepository = experienceRepository;
        this.intentAnalyzer = intentAnalyzer;
        this.intentAnalysisService = intentAnalysisService;
        this.recommendationScorer = recommendationScorer;
    }

    public IntentResponse createIntent(CreateIntentRequest request) {

        Traveler traveler = travelerRepository
            .findById(request.travelerId())
            .orElseThrow(() ->
                new ResourceNotFoundException("Traveler not found")
            );

        ExperienceCategory category = request.category();

        if (category == null) {
            category = intentAnalyzer.detectCategory(request.description());
        }


        Intent intent = new Intent();
        intent.setTitle(request.title());
        intent.setDescription(request.description());
        intent.setStatus(IntentStatus.DRAFT);
        intent.setCategory(category);
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

    public List<ScoredExperienceResponse> getRecommendations(Long intentId) {

        Intent intent = intentRepository
            .findById(intentId)
            .orElseThrow(() ->
                new ResourceNotFoundException("Intent not found")
            );

        AnalyzedIntent analyzed = intentAnalysisService.analyze(
            intent.getDescription()
        );
        AnalyzedIntent effectiveIntent = new AnalyzedIntent(
            analyzed.category() != null
                ? analyzed.category()
                : intent.getCategory(),
            analyzed.activity(),
            analyzed.destination()
        );

        return experienceRepository
            .findAll()
            .stream()
            .map(experience -> {
                int score = recommendationScorer.score(effectiveIntent, experience);
                return new ScoredExperienceResponse(
                    experience.getId(),
                    experience.getTitle(),
                    experience.getDescription(),
                    experience.getCategory(),
                    experience.getDestination(),
                    experience.getDurationDays(),
                    score
                );
            })
            .filter(experience -> experience.score() > 0)
            .sorted((first, second) -> Integer.compare(
                second.score(),
                first.score()
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