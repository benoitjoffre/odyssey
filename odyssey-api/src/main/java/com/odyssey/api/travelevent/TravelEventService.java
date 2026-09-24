package com.odyssey.api.travelevent;

import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.experience.Experience;
import com.odyssey.api.experience.ExperienceRepository;
import com.odyssey.api.trip.TripRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TravelEventService {

    private final TravelEventRepository travelEventRepository;
    private final ExperienceRepository experienceRepository;
    private final TripRepository tripRepository;

    public TravelEventService(
        TravelEventRepository travelEventRepository,
        ExperienceRepository experienceRepository,
        TripRepository tripRepository
    ) {
        this.travelEventRepository = travelEventRepository;
        this.experienceRepository = experienceRepository;
        this.tripRepository = tripRepository;
    }

    public TravelEventResponse create(CreateTravelEventRequest request) {

        if (request.startDate().isAfter(request.endDate())) {
            throw new IllegalArgumentException(
                "Start date cannot be after end date"
            );
        }

        Experience experience = experienceRepository
            .findById(request.experienceId())
            .orElseThrow(() ->
                new ResourceNotFoundException("Experience not found")
            );

        TravelEvent event = new TravelEvent();

        event.setName(request.name());
        event.setLocation(request.location());
        event.setStartDate(request.startDate());
        event.setEndDate(request.endDate());
        event.setDescription(request.description());
        event.setExperience(experience);

        return toResponse(travelEventRepository.save(event));
    }

    public List<TravelEventResponse> getAll() {
        return travelEventRepository
            .findAll()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public TravelEventResponse getById(Long id) {
        return travelEventRepository
            .findById(id)
            .map(this::toResponse)
            .orElseThrow(() ->
                new ResourceNotFoundException("Travel event not found")
            );
    }

    public void delete(Long id) {
        TravelEvent event = travelEventRepository
            .findById(id)
            .orElseThrow(() ->
                new ResourceNotFoundException("Travel event not found")
            );

        if (tripRepository.existsByTravelEventId(id)) {
            throw new IllegalArgumentException(
                "Travel event cannot be deleted while trips are linked to it"
            );
        }

        travelEventRepository.delete(event);
    }

    private TravelEventResponse toResponse(TravelEvent event) {
        return new TravelEventResponse(
            event.getId(),
            event.getName(),
            event.getLocation(),
            event.getStartDate(),
            event.getEndDate(),
            event.getDescription(),
            event.getExperience().getId()
        );
    }
}