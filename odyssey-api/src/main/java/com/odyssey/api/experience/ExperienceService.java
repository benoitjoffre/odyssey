package com.odyssey.api.experience;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.util.StringUtils.hasText;

import com.odyssey.api.destination.Destination;
import com.odyssey.api.destination.DestinationRepository;
import com.odyssey.api.destination.DestinationResponse;
import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.travelevent.TravelEvent;
import com.odyssey.api.travelevent.TravelEventRepository;
import com.odyssey.api.trip.TripRepository;



import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
@Service
public class ExperienceService {

  private static final Logger logger =
      LoggerFactory.getLogger(ExperienceService.class);

  private final DestinationRepository destinationRepository;
  private final ExperienceRepository experienceRepository;
  private final TravelEventRepository travelEventRepository;
  private final TripRepository tripRepository;
  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private static final long CACHE_TTL_SECONDS = 30;

  public ExperienceService(
    DestinationRepository destinationRepository,
    ExperienceRepository experienceRepository,
    TravelEventRepository travelEventRepository,
    TripRepository tripRepository,
    StringRedisTemplate redisTemplate,
    ObjectMapper objectMapper
  ) {
    this.destinationRepository = destinationRepository;
    this.experienceRepository = experienceRepository;
    this.travelEventRepository = travelEventRepository;
    this.tripRepository = tripRepository;
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
  }

  private ExperienceResponse toResponse(Experience experience) {
    return new ExperienceResponse(
        experience.getId(),
        experience.getTitle(),
        experience.getDescription(),
        experience.getCategory(),
        toDestinationResponse(experience),
        experience.getDurationDays()
    );
}

public ExperienceResponse createExperience(CreateExperienceRequest request) {
      Destination destination = destinationRepository
        .findById(request.destinationId())
        .orElseThrow(() ->
          new ResourceNotFoundException("Destination not found")
        );

    Experience experience = new Experience();
    experience.setTitle(request.title());
    experience.setDescription(request.description());
      experience.setDestination(destination);
      experience.setLegacyDestination(destination.getCity());
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

    String cachedValue = null;
    try {
      cachedValue = redisTemplate.opsForValue().get(key);
    } catch (RuntimeException exception) {
      logger.warn("Redis read failed for key {}. Falling back to database.", key, exception);
    }

    if (cachedValue != null) {
      logger.debug("Redis cache hit for experience {}", id);
      try {
        return objectMapper.readValue(cachedValue, ExperienceResponse.class);
      } catch (JacksonException e) {
        logger.warn("Invalid cached payload for key {}. Falling back to database.", key, e);
      }
    }

    logger.debug("Redis cache miss for experience {}", id);

    Experience experience = experienceRepository
        .findById(id)
        .orElseThrow(() ->
            new ResourceNotFoundException("Experience not found")
        );

    ExperienceResponse response = toResponse(experience);
    try {
      String json = objectMapper.writeValueAsString(response);
      try {
        redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(CACHE_TTL_SECONDS));
      } catch (RuntimeException exception) {
        logger.warn("Redis write failed for key {}. Continuing without cache.", key, exception);
      }
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

    Destination destination = destinationRepository
      .findById(request.destinationId())
      .orElseThrow(() ->
        new ResourceNotFoundException("Destination not found")
      );

    experience.setTitle(request.title());
    experience.setDescription(request.description());
    experience.setDestination(destination);
    experience.setLegacyDestination(destination.getCity());
    experience.setCategory(request.category());
    experience.setDurationDays(request.durationDays());

    Experience savedExperience = experienceRepository.save(experience);

    String key = "experience:" + id;
    try {
      redisTemplate.delete(key);
    } catch (RuntimeException exception) {
      logger.warn("Redis delete failed for key {}. Continuing after update.", key, exception);
    }

    return toResponse(savedExperience);
}

  @Transactional
  public void deleteExperience(Long id) {
    Experience experience = experienceRepository
        .findById(id)
        .orElseThrow(() ->
            new ResourceNotFoundException("Experience not found")
        );

    List<TravelEvent> events = travelEventRepository.findByExperienceId(id);
    if (!events.isEmpty()) {
      List<Long> eventIds = events.stream()
          .map(TravelEvent::getId)
          .toList();
      tripRepository.clearTravelEventByTravelEventIds(eventIds);
      travelEventRepository.deleteAll(events);
    }

    experienceRepository.delete(experience);
    String key = "experience:" + id;
    try {
      redisTemplate.delete(key);
    } catch (RuntimeException exception) {
      logger.warn("Redis delete failed for key {}. Continuing after delete.", key, exception);
    }
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
}
