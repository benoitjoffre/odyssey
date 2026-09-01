package com.odyssey.api.provider;

import com.odyssey.api.booking.BookingRequest;
import com.odyssey.api.booking.BookingRequestRepository;
import com.odyssey.api.need.accommodation.AccommodationCriteriaRepository;
import com.odyssey.api.need.flight.FlightCriteriaRepository;
import com.odyssey.api.provider.accommodation.AccommodationSearchRequest;
import com.odyssey.api.provider.accommodation.AccommodationSearchService;
import com.odyssey.api.provider.flight.FlightSearchRequest;
import com.odyssey.api.provider.flight.FlightSearchService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProviderSearchService {

    private final BookingRequestRepository bookingRequestRepository;
    private final FlightSearchService flightSearchService;
    private final AccommodationSearchService accommodationSearchService;
    private final FlightCriteriaRepository flightCriteriaRepository;
    private final AccommodationCriteriaRepository accommodationCriteriaRepository;

    public ProviderSearchService(
            BookingRequestRepository bookingRequestRepository,
            FlightSearchService flightSearchService,
            AccommodationSearchService accommodationSearchService,
            FlightCriteriaRepository flightCriteriaRepository,
            AccommodationCriteriaRepository accommodationCriteriaRepository
    ) {
        this.bookingRequestRepository = bookingRequestRepository;
        this.flightSearchService = flightSearchService;
        this.accommodationSearchService = accommodationSearchService;
        this.flightCriteriaRepository = flightCriteriaRepository;
        this.accommodationCriteriaRepository = accommodationCriteriaRepository;
    }

    public List<? extends ProviderOffer> search(Long bookingRequestId) {

        BookingRequest bookingRequest = bookingRequestRepository
                .findById(bookingRequestId)
                .orElseThrow(() ->
                        new RuntimeException("BookingRequest not found")
                );

        var need = bookingRequest.getNeed();
        var trip = need.getTrip();

        return switch (need.getType()) {

            case FLIGHT -> {
                var flightCriteria = flightCriteriaRepository
                        .findByNeedId(need.getId())
                        .orElseThrow(() ->
                                new RuntimeException("FlightCriteria not found")
                        );

                yield flightSearchService.search(
                        new FlightSearchRequest(
                                flightCriteria.getOrigin(),
                                flightCriteria.getDestination(),
                                trip.getStartDate(),
                                trip.getEndDate(),
                                flightCriteria.getTravelers()
                        )
                );
            }

            case ACCOMMODATION -> {
                var accommodationCriteria = accommodationCriteriaRepository
                        .findByNeedId(need.getId())
                        .orElseThrow(() ->
                                new RuntimeException("AccommodationCriteria not found")
                        );

                yield accommodationSearchService.search(
                        new AccommodationSearchRequest(
                                accommodationCriteria.getCity(),
                                trip.getStartDate(),
                                trip.getEndDate(),
                                accommodationCriteria.getTravelers(),
                                accommodationCriteria.getRooms()
                        )
                );
            }

            default -> throw new IllegalArgumentException(
                    "No provider available for need type: " + need.getType()
            );
        };
    }
}