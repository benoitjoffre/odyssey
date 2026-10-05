package com.odyssey.api.trip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.odyssey.api.booking.BookingRequestRepository;
import com.odyssey.api.booking.confirmation.BookingRepository;
import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.need.NeedRepository;
import com.odyssey.api.payment.Payment;
import com.odyssey.api.payment.PaymentRepository;
import com.odyssey.api.payment.PaymentStatus;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerRepository;
import com.odyssey.api.travelevent.TravelEventRepository;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    private static final String TRAVELER_A_SUBJECT = "auth0|traveler-a";

    @Mock private TripRepository tripRepository;
    @Mock private TravelerRepository travelerRepository;
    @Mock private NeedRepository needRepository;
    @Mock private BookingRequestRepository bookingRequestRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private TravelEventRepository travelEventRepository;
    @Mock private TripAssistanceFeeEligibility tripAssistanceFeeEligibility;
    @Mock private PaymentRepository paymentRepository;

    private TripService tripService;
    private Traveler travelerA;
    private Trip tripA;

    @BeforeEach
    void setUp() {
        tripService = new TripService(
            tripRepository,
            travelerRepository,
            needRepository,
            bookingRequestRepository,
            bookingRepository,
            travelEventRepository,
            tripAssistanceFeeEligibility,
            paymentRepository
        );
        travelerA = traveler(1L, "Alice", "A");
        tripA = trip(10L, travelerA);
    }

    @Test
    void travelerCanGetOwnedTrip() {
        mockTravelerA();
        when(tripRepository.findByIdAndTravelerId(10L, 1L))
            .thenReturn(Optional.of(tripA));

        TripResponse response = tripService.getTrip(10L, TRAVELER_A_SUBJECT);

        assertEquals(10L, response.id());
        assertEquals(1L, response.travelerId());
    }

    @Test
    void travelerCannotGetAnotherTravelersTrip() {
        mockTravelerA();
        when(tripRepository.findByIdAndTravelerId(20L, 1L))
            .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> tripService.getTrip(20L, TRAVELER_A_SUBJECT)
        );

        assertEquals("Trip not found", exception.getMessage());
    }

    @Test
    void travelerCanDeleteOwnedTrip() {
        mockTravelerA();
        when(tripRepository.findByIdAndTravelerId(10L, 1L))
            .thenReturn(Optional.of(tripA));

        tripService.deleteTrip(10L, TRAVELER_A_SUBJECT);

        verify(tripRepository).delete(tripA);
    }

    @Test
    void travelerCannotDeleteAnotherTravelersTrip() {
        mockTravelerA();
        when(tripRepository.findByIdAndTravelerId(20L, 1L))
            .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> tripService.deleteTrip(20L, TRAVELER_A_SUBJECT)
        );

        assertEquals("Trip not found", exception.getMessage());
        verify(tripRepository, never()).delete(any());
    }

    @Test
    void travelerCanGetOwnedTripDetail() {
        mockTravelerA();
        when(tripRepository.findByIdAndTravelerId(10L, 1L))
            .thenReturn(Optional.of(tripA));
        when(needRepository.findByTripId(10L)).thenReturn(List.of());
        when(tripAssistanceFeeEligibility.isAssistanceFeePayable(tripA)).thenReturn(false);
        when(paymentRepository.findFirstByTripIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.empty());

        TripDetailResponse response = tripService.getTripDetail(
            10L,
            TRAVELER_A_SUBJECT
        );

        assertEquals(10L, response.id());
        assertEquals(1L, response.travelerId());
        assertEquals(false, response.assistanceFeePayable());
        assertEquals(null, response.paymentStatus());
    }

    @Test
    void tripDetailReturnsPendingWhenTripPaymentIsPending() {
        mockTravelerA();
        when(tripRepository.findByIdAndTravelerId(10L, 1L))
            .thenReturn(Optional.of(tripA));
        when(needRepository.findByTripId(10L)).thenReturn(List.of());
        when(tripAssistanceFeeEligibility.isAssistanceFeePayable(tripA)).thenReturn(false);

        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findFirstByTripIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.of(payment));

        TripDetailResponse response = tripService.getTripDetail(10L, TRAVELER_A_SUBJECT);

        assertEquals(PaymentStatus.PENDING, response.paymentStatus());
    }

    @Test
    void tripDetailReturnsPaidWhenTripPaymentIsPaid() {
        mockTravelerA();
        when(tripRepository.findByIdAndTravelerId(10L, 1L))
            .thenReturn(Optional.of(tripA));
        when(needRepository.findByTripId(10L)).thenReturn(List.of());
        when(tripAssistanceFeeEligibility.isAssistanceFeePayable(tripA)).thenReturn(true);

        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.PAID);
        when(paymentRepository.findFirstByTripIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.of(payment));

        TripDetailResponse response = tripService.getTripDetail(10L, TRAVELER_A_SUBJECT);

        assertEquals(PaymentStatus.PAID, response.paymentStatus());
    }

    @Test
    void travelerCannotGetAnotherTravelersTripDetail() {
        mockTravelerA();
        when(tripRepository.findByIdAndTravelerId(20L, 1L))
            .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> tripService.getTripDetail(20L, TRAVELER_A_SUBJECT)
        );

        assertEquals("Trip not found", exception.getMessage());
        verify(needRepository, never()).findByTripId(any());
    }

    @Test
    void updateAssistanceFeeAllowsWhenNoPaymentExists() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(tripA));
        when(paymentRepository.findFirstByTripIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.empty());

        TripResponse response = tripService.updateAssistanceFee(10L, new java.math.BigDecimal("150"));

        assertEquals(new java.math.BigDecimal("150"), response.assistanceFee());
        assertEquals(new java.math.BigDecimal("150"), tripA.getAssistanceFee());
    }

    @Test
    void updateAssistanceFeeAllowsWhenLatestPaymentFailed() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(tripA));

        Payment failedPayment = new Payment();
        failedPayment.setStatus(PaymentStatus.FAILED);
        when(paymentRepository.findFirstByTripIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.of(failedPayment));

        TripResponse response = tripService.updateAssistanceFee(10L, new java.math.BigDecimal("180"));

        assertEquals(new java.math.BigDecimal("180"), response.assistanceFee());
        assertEquals(new java.math.BigDecimal("180"), tripA.getAssistanceFee());
    }

    @Test
    void updateAssistanceFeeRejectsWhenLatestPaymentIsPending() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(tripA));

        Payment pendingPayment = new Payment();
        pendingPayment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findFirstByTripIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.of(pendingPayment));

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> tripService.updateAssistanceFee(10L, new java.math.BigDecimal("200"))
        );

        assertEquals("Trip assistance fee cannot be modified while a payment is pending or paid", exception.getMessage());
        assertEquals(new java.math.BigDecimal("0"), tripA.getAssistanceFee());
    }

    @Test
    void updateAssistanceFeeRejectsWhenLatestPaymentIsPaid() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(tripA));

        Payment paidPayment = new Payment();
        paidPayment.setStatus(PaymentStatus.PAID);
        when(paymentRepository.findFirstByTripIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.of(paidPayment));

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> tripService.updateAssistanceFee(10L, new java.math.BigDecimal("220"))
        );

        assertEquals("Trip assistance fee cannot be modified while a payment is pending or paid", exception.getMessage());
        assertEquals(new java.math.BigDecimal("0"), tripA.getAssistanceFee());
    }

    @Test
    void createTripUsesAuthenticatedTraveler() {
        mockTravelerA();
        CreateTripRequest request = new CreateTripRequest(
            "Trip A",
            LocalDate.now().plusDays(10),
            LocalDate.now().plusDays(15),
            null
        );
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
            Trip trip = invocation.getArgument(0);
            ReflectionTestUtils.setField(trip, "id", 10L);
            return trip;
        });

        TripResponse response = tripService.createTrip(
            request,
            TRAVELER_A_SUBJECT
        );

        assertEquals(1L, response.travelerId());
        verify(travelerRepository).findByAuth0Subject(TRAVELER_A_SUBJECT);
    }

    private void mockTravelerA() {
        when(travelerRepository.findByAuth0Subject(TRAVELER_A_SUBJECT))
            .thenReturn(Optional.of(travelerA));
    }

    private Traveler traveler(Long id, String firstName, String lastName) {
        Traveler traveler = new Traveler(
            firstName,
            lastName,
            firstName + "@example.com"
        );
        ReflectionTestUtils.setField(traveler, "id", id);
        return traveler;
    }

    private Trip trip(Long id, Traveler traveler) {
        Trip trip = new Trip();
        ReflectionTestUtils.setField(trip, "id", id);
        trip.setTitle("Trip " + id);
        trip.setStartDate(LocalDate.now().plusDays(10));
        trip.setEndDate(LocalDate.now().plusDays(15));
        trip.setStatus(TripStatus.DRAFT);
        trip.setTraveler(traveler);
        return trip;
    }
}