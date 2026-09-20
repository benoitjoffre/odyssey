package com.odyssey.api.quote;

import com.odyssey.api.agent.Agent;
import com.odyssey.api.agent.AgentRepository;
import com.odyssey.api.agent.AgentStatus;
import com.odyssey.api.booking.BookingRequestResponse;
import com.odyssey.api.booking.BookingRequestService;
import com.odyssey.api.booking.CreateBookingRequest;
import com.odyssey.api.need.Need;
import com.odyssey.api.need.NeedRepository;
import com.odyssey.api.need.NeedStatus;
import com.odyssey.api.need.NeedType;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerRepository;
import com.odyssey.api.trip.Trip;
import com.odyssey.api.trip.TripRepository;
import com.odyssey.api.trip.TripStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration reproduction of E2E-01: a Trip made of FLIGHT, ACCOMMODATION
 * and TRANSFER BookingRequests must become CONFIRMED once every
 * BookingRequest's *current* Quote is ACCEPTED, even when the TRANSFER
 * BookingRequest went through a REJECTED Quote before being re-quoted and
 * accepted. The historical REJECTED Quote must never block confirmation.
 */
@SpringBootTest
@Transactional
class TripConfirmationIntegrationTest {

    @Autowired
    private TravelerRepository travelerRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private NeedRepository needRepository;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private BookingRequestService bookingRequestService;

    @Autowired
    private QuoteService quoteService;

    @Test
    void tripBecomesConfirmedAfterOneServiceIsRejectedThenReQuotedAndAccepted() {
        Traveler traveler = travelerRepository.save(new Traveler(
                "Trip",
                "Confirmation",
                "trip-confirmation-" + UUID.randomUUID() + "@example.com"
        ));

        Trip trip = new Trip();
        trip.setTitle("E2E-01 trip");
        trip.setStartDate(LocalDate.now().plusDays(10));
        trip.setEndDate(LocalDate.now().plusDays(15));
        trip.setStatus(TripStatus.DRAFT);
        trip.setTraveler(traveler);
        trip = tripRepository.save(trip);

        Agent agent = new Agent();
        agent.setFirstName("Confirmation");
        agent.setLastName("Agent");
        agent.setEmail("trip-confirmation-agent-" + UUID.randomUUID() + "@example.com");
        agent.setStatus(AgentStatus.AVAILABLE);
        agent.setAuth0Subject("auth0|trip-confirmation-agent-" + UUID.randomUUID());
        agent = agentRepository.save(agent);

        Need flightNeed = createNeed(trip, NeedType.FLIGHT);
        Need accommodationNeed = createNeed(trip, NeedType.ACCOMMODATION);
        Need transferNeed = createNeed(trip, NeedType.TRANSFER);

        BookingRequestResponse flightRequest = bookingRequestService
                .createBookingRequest(new CreateBookingRequest(flightNeed.getId(), null));
        BookingRequestResponse accommodationRequest = bookingRequestService
                .createBookingRequest(new CreateBookingRequest(accommodationNeed.getId(), null));
        BookingRequestResponse transferRequest = bookingRequestService
                .createBookingRequest(new CreateBookingRequest(transferNeed.getId(), null));

        bookingRequestService.claimBookingRequest(flightRequest.id(), agent.getId());
        bookingRequestService.claimBookingRequest(accommodationRequest.id(), agent.getId());
        bookingRequestService.claimBookingRequest(transferRequest.id(), agent.getId());

        QuoteResponse flightQuote = createQuote(flightRequest.id(), agent.getId());
        QuoteResponse accommodationQuote = createQuote(accommodationRequest.id(), agent.getId());
        QuoteResponse transferQuoteA = createQuote(transferRequest.id(), agent.getId());

        quoteService.sendQuote(flightQuote.id(), agent.getId());
        quoteService.sendQuote(accommodationQuote.id(), agent.getId());
        quoteService.sendQuote(transferQuoteA.id(), agent.getId());

        quoteService.acceptQuote(flightQuote.id(), traveler.getId());
        quoteService.acceptQuote(accommodationQuote.id(), traveler.getId());

        // TRANSFER: the traveler rejects the first proposal.
        quoteService.rejectQuote(transferQuoteA.id(), traveler.getId());

        Trip tripAfterRejection = tripRepository.findById(trip.getId()).orElseThrow();
        assertEquals(
                TripStatus.DRAFT,
                tripAfterRejection.getStatus(),
                "Trip must not be confirmed while TRANSFER has no accepted current Quote"
        );

        // The agent proposes a new TRANSFER Quote, which the traveler
        // accepts. The Trip must now be confirmed: the earlier REJECTED
        // Quote must not keep blocking confirmation.
        QuoteResponse transferQuoteB = createQuote(transferRequest.id(), agent.getId());
        quoteService.sendQuote(transferQuoteB.id(), agent.getId());
        quoteService.acceptQuote(transferQuoteB.id(), traveler.getId());

        Trip confirmedTrip = tripRepository.findById(trip.getId()).orElseThrow();
        assertEquals(TripStatus.CONFIRMED, confirmedTrip.getStatus());
    }

    private Need createNeed(Trip trip, NeedType type) {
        Need need = new Need();
        need.setType(type);
        need.setStatus(NeedStatus.DRAFT);
        need.setTrip(trip);
        return needRepository.save(need);
    }

    private QuoteResponse createQuote(Long bookingRequestId, Long agentId) {
        return quoteService.createQuote(
                bookingRequestId,
                agentId,
                new CreateQuoteRequest(
                        "provider",
                        "offer-" + UUID.randomUUID(),
                        BigDecimal.valueOf(500),
                        BigDecimal.valueOf(50),
                        "EUR",
                        "description",
                        null
                )
        );
    }
}
