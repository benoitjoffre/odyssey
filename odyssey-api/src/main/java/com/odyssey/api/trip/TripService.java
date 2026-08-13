package com.odyssey.api.trip;

import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerRepository;
import com.odyssey.api.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

import java.util.List;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final TravelerRepository travelerRepository;
    private TripResponse toResponse(Trip trip) {
    return new TripResponse(
        trip.getId(),
        trip.getTitle(),
        trip.getStartDate(),
        trip.getEndDate(),
        trip.getStatus(),
        trip.getTraveler().getId()
    );
}

    public TripService(
        TripRepository tripRepository,
        TravelerRepository travelerRepository
    ) {
        this.tripRepository = tripRepository;
        this.travelerRepository = travelerRepository;
    }

    public TripResponse createTrip(CreateTripRequest request) {

        if (request.startDate().isAfter(request.endDate())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        } 

        if (request.startDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }

        Traveler traveler = travelerRepository
            .findById(request.travelerId())
            .orElseThrow(() -> new ResourceNotFoundException("Traveler not found"));

        Trip trip = new Trip();
        trip.setTitle(request.title());
        trip.setStartDate(request.startDate());
        trip.setEndDate(request.endDate());
        trip.setStatus(TripStatus.DRAFT);
        trip.setTraveler(traveler);

        Trip savedTrip = tripRepository.save(trip);

        return toResponse(savedTrip);
    }

    public List<TripResponse> getTrips() {
        return tripRepository.findAll()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public TripResponse getTrip(Long id) {
        Trip trip = tripRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        return toResponse(trip);
    }
}