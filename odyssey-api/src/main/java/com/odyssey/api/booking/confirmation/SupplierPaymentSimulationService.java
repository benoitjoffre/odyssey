package com.odyssey.api.booking.confirmation;

import com.odyssey.api.booking.BookingRequestStatus;
import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.need.NeedStatus;
import com.odyssey.api.quote.QuoteStatus;
import com.odyssey.api.trip.TripStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SupplierPaymentSimulationService {

    private static final Logger logger = LoggerFactory.getLogger(SupplierPaymentSimulationService.class);

    private final BookingRepository bookingRepository;
    private final SupplierPaymentSimulationSettings settings;

    public SupplierPaymentSimulationService(
        BookingRepository bookingRepository, SupplierPaymentSimulationSettings settings
    ) {
        this.bookingRepository = bookingRepository;
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public SupplierPaymentSimulationResponse getPayment(Long bookingId, String subject) {
        requireEnabled();
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        validateBooking(booking, subject);
        return toResponse(booking);
    }

    @Transactional
    public SupplierPaymentSimulationResponse simulatePayment(Long bookingId, String subject) {
        requireEnabled();
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        validateBooking(booking, subject);
        if (booking.getProviderPaymentStatus() != ProviderPaymentStatus.PAID_TO_PROVIDER) {
            booking.setProviderPaymentStatus(ProviderPaymentStatus.PAID_TO_PROVIDER);
            bookingRepository.save(booking);
            logger.info("SIMULATION: supplier payment marked paid for booking {}; no money transferred", bookingId);
        }
        return toResponse(booking);
    }

    private void requireEnabled() {
        if (!settings.isEnabled()) {
            throw new ResourceNotFoundException("Supplier payment simulation is disabled");
        }
    }

    private void validateBooking(Booking booking, String subject) {
        var quote = booking.getQuote();
        var request = quote.getBookingRequest();
        var need = request.getNeed();
        var trip = need.getTrip();
        if (!subject.equals(trip.getTraveler().getAuth0Subject())) {
            throw new ResourceNotFoundException("Booking not found");
        }
        if (trip.getStatus() == TripStatus.CANCELLED
            || need.getStatus() == NeedStatus.CANCELLED
            || request.getStatus() == BookingRequestStatus.CANCELLED
            || quote.getStatus() != QuoteStatus.ACCEPTED
            || (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED)
            || (booking.getProviderPaymentStatus() != ProviderPaymentStatus.PAYMENT_REQUIRED
                && booking.getProviderPaymentStatus() != ProviderPaymentStatus.PAID_TO_PROVIDER)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This booking is not payable");
        }
        if (quote.getProviderPrice() == null || quote.getProviderPrice().signum() <= 0
            || quote.getCurrency() == null || quote.getCurrency().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking price or currency is missing");
        }
    }

    private SupplierPaymentSimulationResponse toResponse(Booking booking) {
        var quote = booking.getQuote();
        var need = quote.getBookingRequest().getNeed();
        var trip = need.getTrip();
        var traveler = trip.getTraveler();
        var flight = need.getFlightCriteria();
        var accommodation = need.getAccommodationCriteria();
        var transfer = need.getTransferCriteria();
        return new SupplierPaymentSimulationResponse(
            booking.getId(), trip.getId(), trip.getTitle(), trip.getStartDate(), trip.getEndDate(),
            traveler.getFirstName(), traveler.getLastName(), traveler.getEmail(),
            quote.getProvider(), booking.getProviderReference(), quote.getDescription(), need.getType(), need.getNotes(),
            flight != null ? flight.getOrigin() : transfer != null ? transfer.getPickupLocation() : null,
            flight != null ? flight.getDestination() : transfer != null ? transfer.getDropoffLocation() : null,
            accommodation != null ? accommodation.getCity() : null,
            flight != null ? flight.getTravelers() : accommodation != null ? accommodation.getTravelers()
                : transfer != null ? transfer.getTravelers() : null,
            accommodation != null ? accommodation.getRooms() : null,
            quote.getProviderPrice(), quote.getCurrency(), booking.getProviderPaymentStatus()
        );
    }
}
