package com.odyssey.api.experience;

import java.util.List;
import org.springframework.stereotype.Service;

import com.odyssey.api.exception.ResourceNotFoundException;



import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
@Service
public class ExperienceService {

  private final ExperienceRepository experienceRepository;
  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private static final long CACHE_TTL_SECONDS = 30;

  public ExperienceService(ExperienceRepository experienceRepository, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
    this.experienceRepository = experienceRepository;
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
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

    String key = "experience:" + id;

    String cachedValue = redisTemplate.opsForValue().get(key);

    if (cachedValue != null) {
      System.out.println("REDIS CACHE HIT - Experience " + id);
      try {
        return objectMapper.readValue(cachedValue, ExperienceResponse.class);
      } catch (JacksonException e) {
        throw new RuntimeException("Failed to deserialize cached value", e);
      }
    }

    System.out.println("REDIS CACHE MISS - Experience " + id);

    Experience experience = experienceRepository
        .findById(id)
        .orElseThrow(() ->
            new ResourceNotFoundException("Experience not found")
        );

    ExperienceResponse response = toResponse(experience);
    try {
      String json = objectMapper.writeValueAsString(response);
      
      redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(CACHE_TTL_SECONDS));
    } catch (JacksonException e) {
      throw new RuntimeException("Failed to serialize response for caching", e);
    }
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

    redisTemplate.delete("experience:" + id);

    return toResponse(savedExperience);
}
}
