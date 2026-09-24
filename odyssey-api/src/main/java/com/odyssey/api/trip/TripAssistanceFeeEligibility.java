package com.odyssey.api.trip;

import com.odyssey.api.booking.BookingRequest;
import com.odyssey.api.booking.BookingRequestRepository;
import com.odyssey.api.booking.BookingRequestStatus;
import com.odyssey.api.booking.confirmation.Booking;
import com.odyssey.api.booking.confirmation.BookingRepository;
import com.odyssey.api.booking.confirmation.BookingStatus;
import com.odyssey.api.booking.confirmation.ProviderPaymentStatus;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Single source of truth for deciding whether a Trip's Odyssey assistance fee
 * is payable.
 */
@Service
public class TripAssistanceFeeEligibility {

    private final BookingRequestRepository bookingRequestRepository;
    private final BookingRepository bookingRepository;

    public TripAssistanceFeeEligibility(
        BookingRequestRepository bookingRequestRepository,
        BookingRepository bookingRepository
    ) {
        this.bookingRequestRepository = bookingRequestRepository;
        this.bookingRepository = bookingRepository;
    }

    public boolean isAssistanceFeePayable(Trip trip) {
        if (trip.getStatus() != TripStatus.CONFIRMED) {
            return false;
        }

        List<BookingRequest> activeBookingRequests = bookingRequestRepository
            .findByNeedTripId(trip.getId())
            .stream()
            .filter(bookingRequest -> bookingRequest.getStatus() != BookingRequestStatus.CANCELLED)
            .toList();

        for (BookingRequest bookingRequest : activeBookingRequests) {
            Booking booking = bookingRepository
                .findByQuoteBookingRequestId(bookingRequest.getId())
                .orElse(null);

            if (booking == null) {
                return false;
            }

            if (booking.getStatus() != BookingStatus.CONFIRMED) {
                return false;
            }

            if (booking.getProviderPaymentStatus() != ProviderPaymentStatus.PAID_TO_PROVIDER) {
                return false;
            }
        }

        return true;
    }
}