package com.odyssey.api.experience;

import java.util.List;
import java.time.Instant;
import org.springframework.stereotype.Service;

import com.odyssey.api.exception.ResourceNotFoundException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExperienceService {

  private final ExperienceRepository experienceRepository;
  private final Map<Long, CacheEntry> cache = new ConcurrentHashMap<>();

  private static final long CACHE_TTL_SECONDS = 10;

  public ExperienceService(ExperienceRepository experienceRepository) {
    this.experienceRepository = experienceRepository;
  }

  private ExperienceResponse toResponse(Experience experience) {
    return new ExperienceResponse(
        experience.getId(),
        experience.getTitle(),
        experience.getDescription(),
        experience.getCategory(),
        experience.getDestination(),
        experience.getDurationDays()
    );
}

public ExperienceResponse createExperience(CreateExperienceRequest request) {
    Experience experience = new Experience();
    experience.setTitle(request.title());
    experience.setDescription(request.description());
    experience.setDestination(request.destination());
    experience.setCategory(request.category());
    experience.setDurationDays(request.durationDays());

    Experience savedExperience = experienceRepository.save(experience);
    return toResponse(savedExperience);
  }
    
  public List<ExperienceResponse> getExperiences() {
    return experienceRepository.findAll()
        .stream()
        .map(this::toResponse)
        .toList();
  }

  public ExperienceResponse getExperience(Long id) {

    CacheEntry cachedEntry = cache.get(id);

    if (cachedEntry != null) {
      if (Instant.now().isBefore(cachedEntry.expiresAt())) {
        System.out.println("CACHE HIT - Experience " + id);
        return cachedEntry.value();
      }

      System.out.println("CACHE EXPIRED - Experience " + id);
      cache.remove(id);
    }

    System.out.println("Cache miss for experience with ID: " + id);

    Experience experience = experienceRepository
        .findById(id)
        .orElseThrow(() ->
            new ResourceNotFoundException("Experience not found")
        );

    ExperienceResponse response = toResponse(experience);
    cache.put(id, new CacheEntry(response, java.time.Instant.now().plusSeconds(CACHE_TTL_SECONDS)));
    return response;
  }

  public ExperienceResponse updateExperience(
    Long id,
    CreateExperienceRequest request
) {
    Experience experience = experienceRepository
        .findById(id)
        .orElseThrow(() ->
            new ResourceNotFoundException("Experience not found")
        );

    experience.setTitle(request.title());
    experience.setDescription(request.description());
    experience.setDestination(request.destination());
    experience.setCategory(request.category());
    experience.setDurationDays(request.durationDays());

    Experience savedExperience = experienceRepository.save(experience);

    cache.remove(id);

    return toResponse(savedExperience);
}
}
