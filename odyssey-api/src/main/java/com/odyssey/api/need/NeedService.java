package com.odyssey.api.need;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.odyssey.api.need.flight.FlightCriteriaRepository;
import com.odyssey.api.need.accommodation.AccommodationCriteria;
import com.odyssey.api.need.accommodation.AccommodationCriteriaRepository;

import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.need.flight.FlightCriteria;
import com.odyssey.api.trip.Trip;
import com.odyssey.api.trip.TripRepository;


@Service
public class NeedService {

    private final NeedRepository needRepository;
    private final TripRepository tripRepository;
    private final FlightCriteriaRepository flightCriteriaRepository;
    private final AccommodationCriteriaRepository accommodationCriteriaRepository;

    public NeedService(
        NeedRepository needRepository,
        TripRepository tripRepository,
        FlightCriteriaRepository flightCriteriaRepository,
        AccommodationCriteriaRepository accommodationCriteriaRepository
    ) {
        this.needRepository = needRepository;
        this.tripRepository = tripRepository;
        this.flightCriteriaRepository = flightCriteriaRepository;
        this.accommodationCriteriaRepository = accommodationCriteriaRepository;
    }
    @Transactional
    public NeedResponse createNeed(CreateNeedRequest request) {

        Trip trip = tripRepository
            .findById(request.tripId())
            .orElseThrow(() ->
                new ResourceNotFoundException("Trip not found")
            );

        if (request.type() == NeedType.FLIGHT
                && request.flightCriteria() == null) {
            throw new IllegalArgumentException(
                "Flight criteria are required for a FLIGHT need"
            );
        }

        Need need = new Need();
        need.setType(request.type());
        need.setNotes(request.notes());
        need.setStatus(NeedStatus.DRAFT);
        need.setTrip(trip);

        Need savedNeed = needRepository.save(need);

        if (request.type() == NeedType.FLIGHT) {

            FlightCriteria criteria = new FlightCriteria();

            criteria.setNeed(savedNeed);
            criteria.setOrigin(request.flightCriteria().origin());
            criteria.setDestination(request.flightCriteria().destination());
            criteria.setTravelers(request.flightCriteria().travelers());

            flightCriteriaRepository.save(criteria);
        }

        if (request.type() == NeedType.ACCOMMODATION) {

            if (request.accommodationCriteria() == null) {
                throw new IllegalArgumentException(
                        "Accommodation criteria are required for an ACCOMMODATION need"
                );
            }

            AccommodationCriteria criteria = new AccommodationCriteria();

            criteria.setNeed(savedNeed);
            criteria.setCity(request.accommodationCriteria().city());
            criteria.setTravelers(request.accommodationCriteria().travelers());
            criteria.setRooms(request.accommodationCriteria().rooms());

            accommodationCriteriaRepository.save(criteria);
        }

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