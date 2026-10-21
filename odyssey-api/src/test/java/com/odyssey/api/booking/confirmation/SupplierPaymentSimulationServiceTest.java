package com.odyssey.api.booking.confirmation;

import com.odyssey.api.booking.BookingRequest;
import com.odyssey.api.booking.BookingRequestStatus;
import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.need.Need;
import com.odyssey.api.need.NeedStatus;
import com.odyssey.api.need.NeedType;
import com.odyssey.api.need.flight.FlightCriteria;
import com.odyssey.api.quote.Quote;
import com.odyssey.api.quote.QuoteStatus;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.trip.Trip;
import com.odyssey.api.trip.TripStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SupplierPaymentSimulationServiceTest {
    private BookingRepository repository;
    private SupplierPaymentSimulationService service;
    private Booking booking;
    private Trip trip;
    private Need need;
    private BookingRequest request;
    private Quote quote;

    @BeforeEach
    void setUp() {
        repository = mock(BookingRepository.class);
        var environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        service = new SupplierPaymentSimulationService(
            repository, new SupplierPaymentSimulationSettings(environment, true));
        Traveler traveler = new Traveler("Marie", "Martin", "marie@example.com");
        traveler.setAuth0Subject("owner");
        trip = new Trip();
        ReflectionTestUtils.setField(trip, "id", 8L);
        trip.setTraveler(traveler);
        trip.setTitle("Voyage à Lisbonne");
        trip.setStartDate(LocalDate.of(2027, 3, 10));
        trip.setEndDate(LocalDate.of(2027, 3, 17));
        trip.setStatus(TripStatus.CONFIRMED);
        need = new Need();
        need.setTrip(trip);
        need.setType(NeedType.FLIGHT);
        need.setStatus(NeedStatus.BOOKED);
        var flight = new FlightCriteria();
        flight.setOrigin("CDG");
        flight.setDestination("LIS");
        flight.setTravelers(2);
        ReflectionTestUtils.setField(need, "flightCriteria", flight);
        request = new BookingRequest();
        request.setNeed(need);
        request.setStatus(BookingRequestStatus.CONFIRMED);
        quote = new Quote();
        quote.setBookingRequest(request);
        quote.setStatus(QuoteStatus.ACCEPTED);
        quote.setProvider("Test Airline");
        quote.setProviderPrice(new BigDecimal("215.50"));
        quote.setCurrency("EUR");
        booking = new Booking();
        ReflectionTestUtils.setField(booking, "id", 42L);
        booking.setQuote(quote);
        booking.setProviderReference("REF-123");
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setProviderPaymentStatus(ProviderPaymentStatus.PAYMENT_REQUIRED);
    }

    @Test
    void showsPersistedDetailsAndOnlyChangesSupplierPaymentStatusOnce() {
        when(repository.findById(42L)).thenReturn(Optional.of(booking));
        when(repository.findByIdForUpdate(42L)).thenReturn(Optional.of(booking));
        var preview = service.getPayment(42L, "owner");
        assertEquals("Voyage à Lisbonne", preview.tripTitle());
        assertEquals("marie@example.com", preview.travelerEmail());
        assertEquals("REF-123", preview.providerReference());
        assertEquals("Test Airline", preview.provider());
        assertEquals("CDG", preview.origin());
        assertEquals("LIS", preview.destination());
        assertEquals(2, preview.travelers());
        assertEquals(new BigDecimal("215.50"), preview.amount());
        assertEquals(ProviderPaymentStatus.PAYMENT_REQUIRED, preview.providerPaymentStatus());
        verify(repository, never()).save(any());

        assertEquals(ProviderPaymentStatus.PAID_TO_PROVIDER, service.simulatePayment(42L, "owner").providerPaymentStatus());
        service.simulatePayment(42L, "owner");
        verify(repository, times(1)).save(booking);
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(new BigDecimal("215.50"), quote.getProviderPrice());
    }

    @Test
    void rejectsOtherTravelerForReadAndWrite() {
        when(repository.findById(42L)).thenReturn(Optional.of(booking));
        when(repository.findByIdForUpdate(42L)).thenReturn(Optional.of(booking));
        assertThrows(ResourceNotFoundException.class, () -> service.getPayment(42L, "other"));
        assertThrows(ResourceNotFoundException.class, () -> service.simulatePayment(42L, "other"));
        verify(repository, never()).save(any());
    }

    @Test
    void disabledServiceNeverReadsOrWritesBookings() {
        var disabled = new SupplierPaymentSimulationService(
            repository, new SupplierPaymentSimulationSettings(new MockEnvironment(), true));
        assertThrows(ResourceNotFoundException.class, () -> disabled.getPayment(42L, "owner"));
        assertThrows(ResourceNotFoundException.class, () -> disabled.simulatePayment(42L, "owner"));
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @EnumSource(value = BookingStatus.class, names = {"CANCELLED", "FAILED"})
    void rejectsInactiveBooking(BookingStatus status) {
        booking.setStatus(status);
        assertNotPayable();
    }

    @Test
    void rejectsCancelledTripNeedOrRequest() {
        trip.setStatus(TripStatus.CANCELLED);
        assertNotPayable();
        trip.setStatus(TripStatus.CONFIRMED);
        need.setStatus(NeedStatus.CANCELLED);
        assertNotPayable();
        need.setStatus(NeedStatus.BOOKED);
        request.setStatus(BookingRequestStatus.CANCELLED);
        assertNotPayable();
    }

    @Test
    void rejectsUnacceptedQuoteOrPaymentNotRequired() {
        quote.setStatus(QuoteStatus.REJECTED);
        assertNotPayable();
        quote.setStatus(QuoteStatus.ACCEPTED);
        booking.setProviderPaymentStatus(ProviderPaymentStatus.NOT_REQUIRED_YET);
        assertNotPayable();
    }

    @Test
    void rejectsInvalidAmount() {
        quote.setProviderPrice(BigDecimal.ZERO);
        assertNotPayable();
    }

    @Test
    void pendingBookingStaysPendingAfterSimulation() {
        booking.setStatus(BookingStatus.PENDING);
        when(repository.findByIdForUpdate(42L)).thenReturn(Optional.of(booking));
        service.simulatePayment(42L, "owner");
        assertEquals(BookingStatus.PENDING, booking.getStatus());
    }

    private void assertNotPayable() {
        when(repository.findByIdForUpdate(42L)).thenReturn(Optional.of(booking));
        var error = assertThrows(ResponseStatusException.class, () -> service.simulatePayment(42L, "owner"));
        assertEquals(409, error.getStatusCode().value());
        verify(repository, never()).save(any());
    }
}
