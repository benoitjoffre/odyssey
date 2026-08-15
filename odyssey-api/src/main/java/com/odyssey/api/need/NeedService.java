package com.odyssey.api.need;

import java.util.List;

import org.springframework.stereotype.Service;

import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.trip.Trip;
import com.odyssey.api.trip.TripRepository;

@Service
public class NeedService {

    private final NeedRepository needRepository;
    private final TripRepository tripRepository;

    public NeedService(
        NeedRepository needRepository,
        TripRepository tripRepository
    ) {
        this.needRepository = needRepository;
        this.tripRepository = tripRepository;
    }

    public NeedResponse createNeed(CreateNeedRequest request) {

        Trip trip = tripRepository
            .findById(request.tripId())
            .orElseThrow(() ->
                new ResourceNotFoundException("Trip not found")
            );

        Need need = new Need();
        need.setType(request.type());
        need.setNotes(request.notes());
        need.setStatus(NeedStatus.DRAFT);
        need.setTrip(trip);

        Need savedNeed = needRepository.save(need);

        return toResponse(savedNeed);
    }

    public NeedResponse getNeed(Long id) {

        Need need = needRepository
            .findById(id)
            .orElseThrow(() ->
                new ResourceNotFoundException("Need not found")
            );

        return toResponse(need);
    }

    public List<NeedResponse> getNeeds() {
        return needRepository.findAll()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public List<NeedResponse> getNeedsByTrip(Long tripId) {

        if (!tripRepository.existsById(tripId)) {
            throw new ResourceNotFoundException("Trip not found");
        }

        return needRepository
            .findByTripId(tripId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private NeedResponse toResponse(Need need) {
        return new NeedResponse(
            need.getId(),
            need.getType(),
            need.getStatus(),
            need.getNotes(),
            need.getTrip().getId()
        );
    }
}