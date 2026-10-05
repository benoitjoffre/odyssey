package com.odyssey.api.intent;

import java.util.List;

import org.springframework.stereotype.Service;
import static org.springframework.util.StringUtils.hasText;

import com.odyssey.api.destination.Destination;
import com.odyssey.api.destination.DestinationResponse;
import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.experience.ExperienceCategory;
import com.odyssey.api.experience.Experience;
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

    public IntentResponse createIntent(
        CreateIntentRequest request,
        String auth0Subject
    ) {

        Traveler traveler = getCurrentTraveler(auth0Subject);

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

    public IntentResponse getIntent(Long id, String auth0Subject) {
        Intent intent = getOwnedIntent(id, auth0Subject);

        return toResponse(intent);
    }

    public List<IntentResponse> getIntents(String auth0Subject) {
        Traveler traveler = getCurrentTraveler(auth0Subject);

        return intentRepository
            .findByTravelerIdOrderByIdDesc(traveler.getId())
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public List<ScoredExperienceResponse> getRecommendations(
        Long intentId,
        String auth0Subject
    ) {

        Intent intent = getOwnedIntent(intentId, auth0Subject);

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
                    toDestinationResponse(experience),
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

    private DestinationResponse toDestinationResponse(Experience experience) {
        Destination destination = experience.getDestinationEntity();
        if (destination != null) {
            return new DestinationResponse(
                destination.getId(),
                destination.getCity(),
                destination.getCountry(),
                destination.getCountryCode()
            );
        }

        if (hasText(experience.getLegacyDestination())) {
            return new DestinationResponse(
                null,
                experience.getLegacyDestination(),
                null,
                null
            );
        }

        return null;
    }

    private Traveler getCurrentTraveler(String auth0Subject) {
        return travelerRepository
            .findByAuth0Subject(auth0Subject)
            .orElseThrow(() ->
                new ResourceNotFoundException("Traveler not found")
            );
    }

    private Intent getOwnedIntent(Long intentId, String auth0Subject) {
        Traveler traveler = getCurrentTraveler(auth0Subject);

        return intentRepository
            .findByIdAndTravelerId(intentId, traveler.getId())
            .orElseThrow(() ->
                new ResourceNotFoundException("Intent not found")
            );
    }
}