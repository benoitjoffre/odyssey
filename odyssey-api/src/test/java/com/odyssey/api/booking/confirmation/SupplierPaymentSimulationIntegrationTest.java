package com.odyssey.api.booking.confirmation;

import com.odyssey.api.booking.BookingRequest;
import com.odyssey.api.booking.BookingRequestRepository;
import com.odyssey.api.booking.BookingRequestStatus;
import com.odyssey.api.need.Need;
import com.odyssey.api.need.NeedRepository;
import com.odyssey.api.need.NeedStatus;
import com.odyssey.api.need.NeedType;
import com.odyssey.api.need.accommodation.AccommodationCriteria;
import com.odyssey.api.need.accommodation.AccommodationCriteriaRepository;
import com.odyssey.api.quote.Quote;
import com.odyssey.api.quote.QuoteRepository;
import com.odyssey.api.quote.QuoteService;
import com.odyssey.api.quote.QuoteStatus;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerRepository;
import com.odyssey.api.trip.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.supplier-payment-simulation.enabled=true")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class SupplierPaymentSimulationIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private TravelerRepository travelers;
    @Autowired private TripRepository trips;
    @Autowired private NeedRepository needs;
    @Autowired private AccommodationCriteriaRepository accommodations;
    @Autowired private BookingRequestRepository requests;
    @Autowired private QuoteRepository quotes;
    @Autowired private BookingRepository bookings;
    @Autowired private QuoteService quoteService;
    @Autowired private TripAssistanceFeeEligibility eligibility;
    @Autowired private EntityManager entityManager;
    @MockitoBean private JwtDecoder jwtDecoder;

    private Booking booking;
    private Long tripId;
    private Long quoteId;
    private Long travelerId;
    private String subject;

    @BeforeEach
    void seed() {
        subject = "simulation-" + UUID.randomUUID();
        Traveler traveler = new Traveler("Alice", "Martin", subject + "@example.com");
        traveler.setAuth0Subject(subject);
        traveler = travelers.save(traveler);
        travelerId = traveler.getId();
        Trip trip = new Trip();
        trip.setTraveler(traveler);
        trip.setTitle("Séjour à Lisbonne");
        trip.setStartDate(LocalDate.of(2027, 4, 10));
        trip.setEndDate(LocalDate.of(2027, 4, 15));
        trip.setStatus(TripStatus.CONFIRMED);
        trip.setAssistanceFee(new BigDecimal("25"));
        trip = trips.save(trip);
        tripId = trip.getId();
        Need need = new Need();
        need.setTrip(trip);
        need.setType(NeedType.ACCOMMODATION);
        need.setStatus(NeedStatus.BOOKED);
        need = needs.save(need);
        AccommodationCriteria accommodation = new AccommodationCriteria();
        accommodation.setNeed(need);
        accommodation.setCity("Lisbonne");
        accommodation.setTravelers(2);
        accommodation.setRooms(1);
        accommodations.save(accommodation);
        BookingRequest request = new BookingRequest();
        request.setNeed(need);
        request.setStatus(BookingRequestStatus.CONFIRMED);
        request = requests.save(request);
        Quote quote = new Quote();
        quote.setBookingRequest(request);
        quote.setProvider("Hotel Demo");
        quote.setExternalOfferId("offer-test");
        quote.setProviderPrice(new BigDecimal("490.00"));
        quote.setAssistanceFee(new BigDecimal("25.00"));
        quote.setTotalAmount(new BigDecimal("515.00"));
        quote.setCurrency("EUR");
        quote.setDescription("Chambre double avec petit-déjeuner");
        quote.setStatus(QuoteStatus.ACCEPTED);
        quote.setCreatedAt(Instant.now());
        quote = quotes.save(quote);
        quoteId = quote.getId();
        booking = new Booking();
        booking.setQuote(quote);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCreatedAt(Instant.now());
        booking.setProviderReference("HOTEL-123");
        booking.setProviderPaymentUrl("https://supplier.example/real-payment");
        booking.setProviderPaymentStatus(ProviderPaymentStatus.PAYMENT_REQUIRED);
        booking = bookings.saveAndFlush(booking);
        authenticate("owner", subject, "TRAVELER");
        authenticate("other", "someone-else", "TRAVELER");
        authenticate("agent", "agent", "AGENT");
        entityManager.clear();
    }

    @Test
    void fullFlowUsesRealDetailsPersistsSupplierPaymentAndUnlocksOnlyExistingFeeEligibility() throws Exception {
        assertFalse(eligibility.isAssistanceFeePayable(trips.findById(tripId).orElseThrow()));
        var quoteResponse = quoteService.getQuotesByTraveler(travelerId).stream()
            .filter(quote -> quote.id().equals(quoteId)).findFirst().orElseThrow();
        assertEquals("/traveler/supplier-payment-simulation/" + booking.getId(), quoteResponse.providerPaymentUrl());

        mvc.perform(get(endpoint()).header("Authorization", "Bearer owner"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.provider").value("Hotel Demo"))
            .andExpect(jsonPath("$.providerReference").value("HOTEL-123"))
            .andExpect(jsonPath("$.city").value("Lisbonne"))
            .andExpect(jsonPath("$.travelers").value(2))
            .andExpect(jsonPath("$.rooms").value(1))
            .andExpect(jsonPath("$.amount").value(490.00))
            .andExpect(jsonPath("$.tripTitle").value("Séjour à Lisbonne"));

        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(post(endpoint()).header("Authorization", "Bearer owner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerPaymentStatus").value("PAID_TO_PROVIDER"))
                .andExpect(jsonPath("$.amount").value(490.00));
        }
        entityManager.flush();
        entityManager.clear();
        var persisted = bookings.findById(booking.getId()).orElseThrow();
        assertEquals(ProviderPaymentStatus.PAID_TO_PROVIDER, persisted.getProviderPaymentStatus());
        assertEquals(BookingStatus.CONFIRMED, persisted.getStatus());
        assertEquals("https://supplier.example/real-payment", persisted.getProviderPaymentUrl());
        assertTrue(eligibility.isAssistanceFeePayable(trips.findById(tripId).orElseThrow()));
        assertEquals(new BigDecimal("25.00"), quotes.findById(quoteId).orElseThrow().getAssistanceFee());
    }

    @Test
    void deniesAnonymousAgentAndOtherTravelerWithoutChangingPayment() throws Exception {
        mvc.perform(get(endpoint())).andExpect(status().isUnauthorized());
        mvc.perform(post(endpoint())).andExpect(status().isUnauthorized());
        for (String method : List.of("GET", "POST")) {
            mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), endpoint())
                    .header("Authorization", "Bearer agent"))
                .andExpect(status().isForbidden());
            mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), endpoint())
                    .header("Authorization", "Bearer other"))
                .andExpect(status().isNotFound());
        }
        assertEquals(ProviderPaymentStatus.PAYMENT_REQUIRED, bookings.findById(booking.getId()).orElseThrow().getProviderPaymentStatus());
    }

    @Test
    void refusesCancelledBookings() throws Exception {
        var current = bookings.findById(booking.getId()).orElseThrow();
        current.setStatus(BookingStatus.CANCELLED);
        bookings.saveAndFlush(current);
        mvc.perform(post(endpoint()).header("Authorization", "Bearer owner"))
            .andExpect(status().isConflict());
        assertEquals(ProviderPaymentStatus.PAYMENT_REQUIRED, current.getProviderPaymentStatus());
    }

    private String endpoint() {
        return "/api/bookings/" + booking.getId() + "/supplier-payment-simulation";
    }

    private void authenticate(String token, String authSubject, String role) {
        Instant now = Instant.now();
        when(jwtDecoder.decode(token)).thenReturn(new Jwt(
            token, now, now.plusSeconds(3600), Map.of("alg", "none"),
            Map.of("sub", authSubject, "https://odyssey.app/roles", List.of(role))
        ));
    }
}
