package com.odyssey.api.event;

import org.springframework.stereotype.Component;
import com.odyssey.api.agent.AgentRepository;
import com.odyssey.api.booking.BookingRequestRepository;
import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.quote.QuoteRepository;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerRepository;
import com.odyssey.api.trip.Trip;
import com.odyssey.api.trip.TripRepository;

/**
 * Prepares traveler messages inside the durable email queue, independently
 * from in-app notification listeners.
 */
@Component
public class ClientEmailEventListener {

    private final TravelerRepository travelerRepository;
    private final AgentRepository agentRepository;
    private final QuoteRepository quoteRepository;
    private final TripRepository tripRepository;
    private final OdysseyEmailRenderer renderer;
    private final BookingRequestRepository bookingRequestRepository;

    public ClientEmailEventListener(TravelerRepository travelerRepository, AgentRepository agentRepository,
        QuoteRepository quoteRepository, TripRepository tripRepository, OdysseyEmailRenderer renderer,
        BookingRequestRepository bookingRequestRepository) {
        this.travelerRepository = travelerRepository;
        this.agentRepository = agentRepository;
        this.quoteRepository = quoteRepository;
        this.tripRepository = tripRepository;
        this.renderer = renderer;
        this.bookingRequestRepository = bookingRequestRepository;
    }

    public EmailMessage onBookingRequested(BookingRequestedEvent event) {
        Traveler traveler = traveler(event.travelerId());
        Trip trip = requestTrip(event.bookingRequestId());
        return renderer.travelerBookingRequested(traveler.getEmail(), traveler.getPreferredLanguage(),
            traveler.getFirstName(), event.bookingRequestId(), trip.getId(), trip.getTitle());
    }

    public EmailMessage onBookingAssigned(BookingAssignedEvent event) {
        Traveler traveler = traveler(event.travelerId());
        var agent = agentRepository.findById(event.agentId())
            .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));
        Trip trip = requestTrip(event.bookingRequestId());
        return renderer.travelerBookingAssigned(traveler.getEmail(), traveler.getPreferredLanguage(),
            traveler.getFirstName(), event.bookingRequestId(), agent.getFirstName(), trip.getId(), trip.getTitle());
    }

    public EmailMessage onQuoteSent(QuoteSentEvent event) {
        Traveler traveler = traveler(event.travelerId());
        var quote = quoteRepository.findById(event.quoteId())
            .orElseThrow(() -> new ResourceNotFoundException("Quote not found"));
        Trip trip = quote.getBookingRequest().getNeed().getTrip();
        return renderer.travelerQuoteSent(traveler.getEmail(), traveler.getPreferredLanguage(),
            traveler.getFirstName(), event.bookingRequestId(), quote.getTotalAmount(),
            quote.getCurrency(), quote.getDescription(), trip.getId(), trip.getTitle());
    }

    public EmailMessage onTripQuotesSent(TripQuotesSentEvent event) {
        Traveler traveler = traveler(event.travelerId());
        Trip trip = trip(event.tripId());
        return renderer.travelerTripQuotesSent(traveler.getEmail(), traveler.getPreferredLanguage(),
            traveler.getFirstName(), trip.getTitle(), event.quoteIds().size(), trip.getId());
    }

    public EmailMessage onTravelerOnboardingCompleted(TravelerOnboardingCompletedEvent event) {
        Traveler traveler = traveler(event.travelerId());
        return renderer.travelerOnboardingCompleted(traveler.getEmail(), traveler.getPreferredLanguage(),
            traveler.getFirstName());
    }

    public EmailMessage onPaymentSucceeded(PaymentSucceededEvent event) {
        Traveler traveler = traveler(event.travelerId());
        Trip trip = trip(event.tripId());
        return renderer.travelerPaymentSucceeded(traveler.getEmail(), traveler.getPreferredLanguage(),
            traveler.getFirstName(), trip.getId(), trip.getTitle(), event.assistanceFee(), event.currency());
    }

    public EmailMessage onTripBookingsConfirmed(TripBookingsConfirmedEvent event) {
        Traveler traveler = traveler(event.travelerId());
        return renderer.travelerTripBookingsConfirmed(traveler.getEmail(), traveler.getPreferredLanguage(),
            traveler.getFirstName(), event.tripId(), event.tripTitle(), event.bookingCount());
    }

    public EmailMessage onProviderPaymentRequired(ProviderPaymentRequiredEvent event) {
        Traveler traveler = traveler(event.travelerId());
        return renderer.travelerProviderPaymentRequired(traveler.getEmail(), traveler.getPreferredLanguage(),
            traveler.getFirstName(), event.tripId(), event.tripTitle());
    }

    private Traveler traveler(Long id) {
        return travelerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Traveler not found"));
    }

    private Trip trip(Long id) {
        return tripRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
    }

    private Trip requestTrip(Long id) {
        return bookingRequestRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Booking request not found")).getNeed().getTrip();
    }
}
