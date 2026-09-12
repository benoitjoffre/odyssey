package com.odyssey.api.provider;

import com.odyssey.api.booking.BookingRequest;
import com.odyssey.api.booking.BookingRequestRepository;
import com.odyssey.api.need.accommodation.AccommodationCriteriaRepository;
import com.odyssey.api.need.flight.FlightCriteriaRepository;
import com.odyssey.api.provider.accommodation.AccommodationSearchRequest;
import com.odyssey.api.provider.accommodation.AccommodationSearchService;
import com.odyssey.api.provider.flight.FlightSearchRequest;
import com.odyssey.api.provider.flight.FlightSearchService;
import com.odyssey.api.provider.transfer.TransferSearchRequest;
import com.odyssey.api.provider.transfer.TransferSearchService;
import com.odyssey.api.need.transfer.TransferCriteriaRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProviderSearchService {

    private final BookingRequestRepository bookingRequestRepository;
    private final FlightSearchService flightSearchService;
    private final AccommodationSearchService accommodationSearchService;
    private final FlightCriteriaRepository flightCriteriaRepository;
    private final AccommodationCriteriaRepository accommodationCriteriaRepository;
    private final TransferSearchService transferSearchService;
    private final TransferCriteriaRepository transferCriteriaRepository;

    public ProviderSearchService(
            BookingRequestRepository bookingRequestRepository,
            FlightSearchService flightSearchService,
            AccommodationSearchService accommodationSearchService,
            FlightCriteriaRepository flightCriteriaRepository,
            AccommodationCriteriaRepository accommodationCriteriaRepository,
            TransferSearchService transferSearchService,
            TransferCriteriaRepository transferCriteriaRepository
    ) {
        this.bookingRequestRepository = bookingRequestRepository;
        this.flightSearchService = flightSearchService;
        this.accommodationSearchService = accommodationSearchService;
        this.flightCriteriaRepository = flightCriteriaRepository;
        this.accommodationCriteriaRepository = accommodationCriteriaRepository;
        this.transferSearchService = transferSearchService;
        this.transferCriteriaRepository = transferCriteriaRepository;
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

            case TRANSFER -> {
                var transferCriteria = transferCriteriaRepository
                        .findByNeedId(need.getId())
                        .orElseThrow(() ->
                                new RuntimeException("TransferCriteria not found")
                        );

                yield transferSearchService.search(
                        new TransferSearchRequest(
                                transferCriteria.getPickupLocation(),
                                transferCriteria.getDropoffLocation(),
                                trip.getStartDate(),
                                transferCriteria.getTravelers()
                        )
                );
            }

            default -> throw new IllegalArgumentException(
                    "No provider available for need type: " + need.getType()
            );
        };
    }
}