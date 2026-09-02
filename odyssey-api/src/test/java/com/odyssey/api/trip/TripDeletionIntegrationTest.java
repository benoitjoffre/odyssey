package com.odyssey.api.trip;

import com.odyssey.api.agent.Agent;
import com.odyssey.api.agent.AgentNotification;
import com.odyssey.api.agent.AgentNotificationRepository;
import com.odyssey.api.agent.AgentRepository;
import com.odyssey.api.agent.AgentStatus;
import com.odyssey.api.booking.BookingRequest;
import com.odyssey.api.booking.BookingRequestRepository;
import com.odyssey.api.booking.BookingRequestStatus;
import com.odyssey.api.booking.confirmation.Booking;
import com.odyssey.api.booking.confirmation.BookingRepository;
import com.odyssey.api.booking.confirmation.BookingStatus;
import com.odyssey.api.need.Need;
import com.odyssey.api.need.NeedRepository;
import com.odyssey.api.need.NeedStatus;
import com.odyssey.api.need.NeedType;
import com.odyssey.api.need.accommodation.AccommodationCriteria;
import com.odyssey.api.need.accommodation.AccommodationCriteriaRepository;
import com.odyssey.api.need.flight.FlightCriteria;
import com.odyssey.api.need.flight.FlightCriteriaRepository;
import com.odyssey.api.quote.Quote;
import com.odyssey.api.quote.QuoteRepository;
import com.odyssey.api.quote.QuoteStatus;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@Transactional
class TripDeletionIntegrationTest {

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TravelerRepository travelerRepository;

    @Autowired
    private NeedRepository needRepository;

    @Autowired
    private AccommodationCriteriaRepository accommodationCriteriaRepository;

    @Autowired
    private FlightCriteriaRepository flightCriteriaRepository;

    @Autowired
    private BookingRequestRepository bookingRequestRepository;

    @Autowired
    private QuoteRepository quoteRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private AgentNotificationRepository agentNotificationRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void deleteTripAlsoDeletesItsAggregate() {
        Traveler traveler = travelerRepository.save(new Traveler(
            "Delete",
            "Test",
            "delete-test-" + UUID.randomUUID() + "@example.com"
        ));

        Trip trip = new Trip();
        trip.setTitle("Trip to delete");
        trip.setStartDate(LocalDate.now().plusDays(1));
        trip.setEndDate(LocalDate.now().plusDays(2));
        trip.setStatus(TripStatus.DRAFT);
        trip.setTraveler(traveler);
        trip = tripRepository.save(trip);

        Need need = new Need();
        need.setType(NeedType.FLIGHT);
        need.setStatus(NeedStatus.DRAFT);
        need.setTrip(trip);
        need = needRepository.save(need);

        AccommodationCriteria accommodationCriteria = new AccommodationCriteria();
        accommodationCriteria.setNeed(need);
        accommodationCriteria.setCity("Paris");
        accommodationCriteria.setTravelers(1);
        accommodationCriteria.setRooms(1);
        accommodationCriteria = accommodationCriteriaRepository.save(accommodationCriteria);

        FlightCriteria flightCriteria = new FlightCriteria();
        flightCriteria.setNeed(need);
        flightCriteria.setOrigin("CDG");
        flightCriteria.setDestination("LHR");
        flightCriteria.setTravelers(1);
        flightCriteria = flightCriteriaRepository.save(flightCriteria);

        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setNeed(need);
        bookingRequest.setStatus(BookingRequestStatus.REQUESTED);
        bookingRequest = bookingRequestRepository.save(bookingRequest);

        Quote quote = new Quote();
        quote.setBookingRequest(bookingRequest);
        quote.setProvider("test-provider");
        quote.setExternalOfferId("test-offer");
        quote.setProviderPrice(BigDecimal.TEN);
        quote.setSellingPrice(BigDecimal.TEN);
        quote.setCurrency("EUR");
        quote.setStatus(QuoteStatus.ACCEPTED);
        quote.setCreatedAt(Instant.now());
        quote = quoteRepository.save(quote);

        Booking booking = new Booking();
        booking.setQuote(quote);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCreatedAt(Instant.now());
        booking = bookingRepository.save(booking);

        Agent agent = new Agent();
        agent.setFirstName("Delete");
        agent.setLastName("Test");
        agent.setEmail("delete-agent-" + UUID.randomUUID() + "@example.com");
        agent.setStatus(AgentStatus.AVAILABLE);
        agent = agentRepository.save(agent);

        AgentNotification notification = new AgentNotification();
        notification.setAgent(agent);
        notification.setBookingRequest(bookingRequest);
        notification.setMessage("Test notification");
        notification.setCreatedAt(Instant.now());
        notification = agentNotificationRepository.save(notification);

        Long tripId = trip.getId();
        Long needId = need.getId();
        Long accommodationCriteriaId = accommodationCriteria.getId();
        Long flightCriteriaId = flightCriteria.getId();
        Long bookingRequestId = bookingRequest.getId();
        Long quoteId = quote.getId();
        Long bookingId = booking.getId();
        Long notificationId = notification.getId();
        Long travelerId = traveler.getId();
        Long agentId = agent.getId();
        entityManager.flush();
        entityManager.clear();

        tripRepository.delete(tripRepository.findById(tripId).orElseThrow());
        tripRepository.flush();

        assertFalse(tripRepository.existsById(tripId));
        assertFalse(needRepository.existsById(needId));
        assertFalse(accommodationCriteriaRepository.existsById(accommodationCriteriaId));
        assertFalse(flightCriteriaRepository.existsById(flightCriteriaId));
        assertFalse(bookingRequestRepository.existsById(bookingRequestId));
        assertFalse(quoteRepository.existsById(quoteId));
        assertFalse(bookingRepository.existsById(bookingId));
        assertFalse(agentNotificationRepository.existsById(notificationId));
        org.junit.jupiter.api.Assertions.assertTrue(travelerRepository.existsById(travelerId));
        org.junit.jupiter.api.Assertions.assertTrue(agentRepository.existsById(agentId));
    }
}