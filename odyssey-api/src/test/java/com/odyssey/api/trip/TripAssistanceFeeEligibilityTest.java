package com.odyssey.api.trip;

import com.odyssey.api.booking.BookingRequest;
import com.odyssey.api.booking.BookingRequestRepository;
import com.odyssey.api.booking.BookingRequestStatus;
import com.odyssey.api.booking.confirmation.Booking;
import com.odyssey.api.booking.confirmation.BookingRepository;
import com.odyssey.api.booking.confirmation.BookingStatus;
import com.odyssey.api.booking.confirmation.ProviderPaymentStatus;
import com.odyssey.api.need.Need;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripAssistanceFeeEligibilityTest {

    private static final long TRIP_ID = 42L;

    @Mock
    private BookingRequestRepository bookingRequestRepository;

    @Mock
    private BookingRepository bookingRepository;

    private TripAssistanceFeeEligibility eligibility;

    @BeforeEach
    void setUp() {
        eligibility = new TripAssistanceFeeEligibility(
            bookingRequestRepository,
            bookingRepository
        );
    }

    @Test
    void returnsFalseWhenTripIsNotConfirmed() {
        Trip trip = trip(TripStatus.DRAFT);

        assertFalse(eligibility.isAssistanceFeePayable(trip));
        verify(bookingRequestRepository, never()).findByNeedTripId(TRIP_ID);
    }

    @Test
    void returnsFalseWhenActiveBookingRequestHasNoBooking() {
        Trip trip = trip(TripStatus.CONFIRMED);
        BookingRequest activeRequest = bookingRequest(11L, BookingRequestStatus.IN_PROGRESS);

        when(bookingRequestRepository.findByNeedTripId(TRIP_ID)).thenReturn(List.of(activeRequest));
        when(bookingRepository.findByQuoteBookingRequestId(11L)).thenReturn(Optional.empty());

        assertFalse(eligibility.isAssistanceFeePayable(trip));
    }

    @Test
    void returnsFalseWhenBookingIsPending() {
        Trip trip = trip(TripStatus.CONFIRMED);
        BookingRequest activeRequest = bookingRequest(12L, BookingRequestStatus.REQUESTED);

        when(bookingRequestRepository.findByNeedTripId(TRIP_ID)).thenReturn(List.of(activeRequest));
        when(bookingRepository.findByQuoteBookingRequestId(12L)).thenReturn(Optional.of(
            booking(BookingStatus.PENDING, ProviderPaymentStatus.PAID_TO_PROVIDER)
        ));

        assertFalse(eligibility.isAssistanceFeePayable(trip));
    }

    @Test
    void returnsFalseWhenBookingIsConfirmedButProviderPaymentRequired() {
        Trip trip = trip(TripStatus.CONFIRMED);
        BookingRequest activeRequest = bookingRequest(13L, BookingRequestStatus.IN_PROGRESS);

        when(bookingRequestRepository.findByNeedTripId(TRIP_ID)).thenReturn(List.of(activeRequest));
        when(bookingRepository.findByQuoteBookingRequestId(13L)).thenReturn(Optional.of(
            booking(BookingStatus.CONFIRMED, ProviderPaymentStatus.PAYMENT_REQUIRED)
        ));

        assertFalse(eligibility.isAssistanceFeePayable(trip));
    }

    @Test
    void returnsTrueWhenAllActiveBookingsAreConfirmedAndPaidToProvider() {
        Trip trip = trip(TripStatus.CONFIRMED);
        BookingRequest firstActive = bookingRequest(14L, BookingRequestStatus.IN_PROGRESS);
        BookingRequest secondActive = bookingRequest(15L, BookingRequestStatus.REQUESTED);

        when(bookingRequestRepository.findByNeedTripId(TRIP_ID)).thenReturn(List.of(firstActive, secondActive));
        when(bookingRepository.findByQuoteBookingRequestId(14L)).thenReturn(Optional.of(
            booking(BookingStatus.CONFIRMED, ProviderPaymentStatus.PAID_TO_PROVIDER)
        ));
        when(bookingRepository.findByQuoteBookingRequestId(15L)).thenReturn(Optional.of(
            booking(BookingStatus.CONFIRMED, ProviderPaymentStatus.PAID_TO_PROVIDER)
        ));

        assertTrue(eligibility.isAssistanceFeePayable(trip));
    }

    @Test
    void ignoresCancelledBookingRequests() {
        Trip trip = trip(TripStatus.CONFIRMED);
        BookingRequest activeRequest = bookingRequest(16L, BookingRequestStatus.IN_PROGRESS);
        BookingRequest cancelledRequest = bookingRequest(17L, BookingRequestStatus.CANCELLED);

        when(bookingRequestRepository.findByNeedTripId(TRIP_ID)).thenReturn(List.of(activeRequest, cancelledRequest));
        when(bookingRepository.findByQuoteBookingRequestId(16L)).thenReturn(Optional.of(
            booking(BookingStatus.CONFIRMED, ProviderPaymentStatus.PAID_TO_PROVIDER)
        ));

        assertTrue(eligibility.isAssistanceFeePayable(trip));
        verify(bookingRepository, never()).findByQuoteBookingRequestId(17L);
    }

    private Trip trip(TripStatus status) {
        Trip trip = new Trip();
        ReflectionTestUtils.setField(trip, "id", TRIP_ID);
        trip.setStatus(status);
        return trip;
    }

    private BookingRequest bookingRequest(Long id, BookingRequestStatus status) {
        BookingRequest bookingRequest = new BookingRequest();
        ReflectionTestUtils.setField(bookingRequest, "id", id);
        bookingRequest.setStatus(status);

        Need need = new Need();
        need.setTrip(new Trip());
        bookingRequest.setNeed(need);

        return bookingRequest;
    }

    private Booking booking(BookingStatus bookingStatus, ProviderPaymentStatus providerPaymentStatus) {
        Booking booking = new Booking();
        booking.setStatus(bookingStatus);
        booking.setProviderPaymentStatus(providerPaymentStatus);
        return booking;
    }
}
