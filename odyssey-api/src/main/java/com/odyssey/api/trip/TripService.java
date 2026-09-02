package com.odyssey.api.trip;

import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerRepository;
import com.odyssey.api.travelevent.TravelEvent;
import com.odyssey.api.travelevent.TravelEventRepository;
import com.odyssey.api.booking.BookingRequestRepository;
import com.odyssey.api.booking.confirmation.BookingRepository;
import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.need.NeedRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

import java.util.List;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final TravelerRepository travelerRepository;
    private final NeedRepository needRepository;
    private final BookingRequestRepository bookingRequestRepository;
    private final BookingRepository bookingRepository;
    private final TravelEventRepository travelEventRepository;

    private TripResponse toResponse(Trip trip) {
    return new TripResponse(
        trip.getId(),
        trip.getTitle(),
        trip.getStartDate(),
        trip.getEndDate(),
        trip.getStatus(),
        trip.getTraveler().getId(),
        trip.getTravelEvent() != null
            ? trip.getTravelEvent().getId()
            : null
    );
}

    public TripService(
        TripRepository tripRepository,
        TravelerRepository travelerRepository,
        NeedRepository needRepository,
        BookingRequestRepository bookingRequestRepository,
        BookingRepository bookingRepository,
        TravelEventRepository travelEventRepository
    ) {
        this.tripRepository = tripRepository;
        this.travelerRepository = travelerRepository;
        this.needRepository = needRepository;
        this.bookingRequestRepository = bookingRequestRepository;
        this.bookingRepository = bookingRepository;
        this.travelEventRepository = travelEventRepository;
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

        TravelEvent travelEvent = null;

        if (request.travelEventId() != null) {
            travelEvent = travelEventRepository
                .findById(request.travelEventId())
                .orElseThrow(() ->
                    new ResourceNotFoundException("Travel event not found")
                );
        }

        if (travelEvent != null) {

            if (request.startDate().isAfter(travelEvent.getStartDate())) {
                throw new IllegalArgumentException(
                    "Trip must start before or on the travel event start date"
                );
            }

            if (request.endDate().isBefore(travelEvent.getEndDate())) {
                throw new IllegalArgumentException(
                    "Trip must end after or on the travel event end date"
                );
            }
        }

        Trip trip = new Trip();
        trip.setTitle(request.title());
        trip.setStartDate(request.startDate());
        trip.setEndDate(request.endDate());
        trip.setStatus(TripStatus.DRAFT);
        trip.setTraveler(traveler);
        trip.setTravelEvent(travelEvent);

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

    @Transactional
    public void deleteTrip(Long id) {
        Trip trip = tripRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        tripRepository.delete(trip);
    }

    public List<TripResponse> getTripsByTraveler(Long travelerId) {

        if (!travelerRepository.existsById(travelerId)) {
            throw new ResourceNotFoundException(
                "Traveler not found"
            );
        }

        return tripRepository
            .findByTravelerIdOrderByStartDateDesc(travelerId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public TripDetailResponse getTripDetail(Long id) {

        Trip trip = tripRepository
            .findById(id)
            .orElseThrow(() ->
                new ResourceNotFoundException("Trip not found")
            );

            

        List<TripNeedResponse> needs =
            needRepository.findByTripId(id)
                .stream()
                .map(need -> {

                    var bookingRequest =
                        bookingRequestRepository
                            .findByNeedId(need.getId())
                            .orElse(null);

                    var booking =
                        bookingRequest == null
                            ? null
                            : bookingRepository
                                .findByQuoteBookingRequestId(
                                    bookingRequest.getId()
                                )
                                .orElse(null);

                    return new TripNeedResponse(
                        need.getId(),
                        need.getType(),
                        need.getStatus(),
                        need.getNotes(),

                        bookingRequest != null
                            ? bookingRequest.getStatus()
                            : null,

                        booking != null
                            ? booking.getStatus()
                            : null,

                        booking != null
                            ? booking.getProviderConfirmationId()
                            : null
                    );
                })
                .toList();

        return new TripDetailResponse(
            trip.getId(),
            trip.getTitle(),
            trip.getStartDate(),
            trip.getEndDate(),
            trip.getStatus(),
            trip.getTraveler().getId(),
            needs,
            trip.getTravelEvent() != null
                ? trip.getTravelEvent().getId()
                : null
        );
    }
}