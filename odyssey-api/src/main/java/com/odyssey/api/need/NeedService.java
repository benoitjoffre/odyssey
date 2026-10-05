package com.odyssey.api.need;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.odyssey.api.need.flight.FlightCriteriaRepository;
import com.odyssey.api.need.accommodation.AccommodationCriteria;
import com.odyssey.api.need.accommodation.AccommodationCriteriaRepository;

import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.need.flight.FlightCriteria;
import com.odyssey.api.need.transfer.TransferCriteria;
import com.odyssey.api.need.transfer.TransferCriteriaRepository;
import com.odyssey.api.need.transfer.TransferCriteriaResponse;
import com.odyssey.api.trip.Trip;
import com.odyssey.api.trip.TripRepository;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerRepository;


@Service
public class NeedService {

    private final NeedRepository needRepository;
    private final TripRepository tripRepository;
    private final TravelerRepository travelerRepository;
    private final FlightCriteriaRepository flightCriteriaRepository;
    private final AccommodationCriteriaRepository accommodationCriteriaRepository;
    private final TransferCriteriaRepository transferCriteriaRepository;

    public NeedService(
        NeedRepository needRepository,
        TripRepository tripRepository,
        TravelerRepository travelerRepository,
        FlightCriteriaRepository flightCriteriaRepository,
        AccommodationCriteriaRepository accommodationCriteriaRepository,
        TransferCriteriaRepository transferCriteriaRepository
    ) {
        this.needRepository = needRepository;
        this.tripRepository = tripRepository;
        this.travelerRepository = travelerRepository;
        this.flightCriteriaRepository = flightCriteriaRepository;
        this.accommodationCriteriaRepository = accommodationCriteriaRepository;
        this.transferCriteriaRepository = transferCriteriaRepository;
    }

    @Transactional
    public NeedResponse createNeed(
        CreateNeedRequest request,
        String auth0Subject
    ) {

        Trip trip = getOwnedTrip(request.tripId(), auth0Subject);

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

        if (request.type() == NeedType.TRANSFER) {

            if (request.transferCriteria() == null) {
                throw new IllegalArgumentException(
                        "Transfer criteria are required for a TRANSFER need"
                );
            }

            TransferCriteria criteria = new TransferCriteria();

            criteria.setNeed(savedNeed);
            criteria.setPickupLocation(request.transferCriteria().pickupLocation());
            criteria.setDropoffLocation(request.transferCriteria().dropoffLocation());
            criteria.setTravelers(request.transferCriteria().travelers());

            transferCriteriaRepository.save(criteria);
        }

        return toResponse(savedNeed);
    }

    public NeedResponse getNeed(Long id, String auth0Subject) {

        Need need = getOwnedNeed(id, auth0Subject);

        return toResponse(need);
    }

    public List<NeedResponse> getNeeds(String auth0Subject) {
        Traveler traveler = getCurrentTraveler(auth0Subject);

        return tripRepository
            .findByTravelerIdOrderByStartDateDesc(traveler.getId())
            .stream()
            .flatMap(trip -> needRepository.findByTripId(trip.getId()).stream())
            .map(this::toResponse)
            .toList();
    }

    public List<NeedResponse> getNeedsByTrip(
        Long tripId,
        String auth0Subject
    ) {

        getOwnedTrip(tripId, auth0Subject);

        return needRepository
            .findByTripId(tripId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public NeedResponse updateNotes(
        Long id,
        UpdateNeedNotesRequest request,
        String auth0Subject
    ) {
        Need need = getOwnedNeed(id, auth0Subject);

        need.setNotes(request.getNotes());

        Need savedNeed = needRepository.save(need);

        return toResponse(savedNeed);
    }

    private TransferCriteriaResponse toTransferCriteriaResponse(
        TransferCriteria criteria
    ) {
        if (criteria == null) {
            return null;
        }

        return new TransferCriteriaResponse(
            criteria.getPickupLocation(),
            criteria.getDropoffLocation(),
            criteria.getTravelers()
        );
    }

    private NeedResponse toResponse(Need need) {
        return new NeedResponse(
            need.getId(),
            need.getType(),
            need.getStatus(),
            need.getNotes(),
            need.getTrip().getId(),
            toTransferCriteriaResponse(need.getTransferCriteria())
        );
    }

    private Traveler getCurrentTraveler(String auth0Subject) {
        return travelerRepository
            .findByAuth0Subject(auth0Subject)
            .orElseThrow(() -> new ResourceNotFoundException("Traveler not found"));
    }

    private Trip getOwnedTrip(Long tripId, String auth0Subject) {
        Traveler traveler = getCurrentTraveler(auth0Subject);

        return tripRepository
            .findByIdAndTravelerId(tripId, traveler.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
    }

    private Need getOwnedNeed(Long needId, String auth0Subject) {
        Traveler traveler = getCurrentTraveler(auth0Subject);

        Need need = needRepository
            .findById(needId)
            .orElseThrow(() -> new ResourceNotFoundException("Need not found"));

        Long ownerId = need.getTrip().getTraveler().getId();
        if (!ownerId.equals(traveler.getId())) {
            throw new ResourceNotFoundException("Need not found");
        }

        return need;
    }


}