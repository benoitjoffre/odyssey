package com.odyssey.api.event;

import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class EmailNotificationPlanner {

    private final ClientEmailEventListener client;
    private final AgentEmailEventListener agent;
    private final ObjectMapper mapper;

    public EmailNotificationPlanner(ClientEmailEventListener client, AgentEmailEventListener agent, ObjectMapper mapper) {
        this.client = client;
        this.agent = agent;
        this.mapper = mapper;
    }

    public List<EmailMessage> prepare(String type, String payload) {
        return switch (type) {
            case "TRAVELER:BOOKING_REQUESTED" ->
                List.of(client.onBookingRequested(mapper.readValue(payload, BookingRequestedEvent.class)));
            case "TRAVELER:BOOKING_ASSIGNED" ->
                List.of(client.onBookingAssigned(mapper.readValue(payload, BookingAssignedEvent.class)));
            case "TRAVELER:QUOTE_SENT" ->
                List.of(client.onQuoteSent(mapper.readValue(payload, QuoteSentEvent.class)));
            case "TRAVELER:TRIP_QUOTES_SENT" ->
                List.of(client.onTripQuotesSent(mapper.readValue(payload, TripQuotesSentEvent.class)));
            case "TRAVELER:TRAVELER_ONBOARDING_COMPLETED" ->
                List.of(client.onTravelerOnboardingCompleted(mapper.readValue(payload, TravelerOnboardingCompletedEvent.class)));
            case "TRAVELER:PAYMENT_SUCCEEDED" ->
                List.of(client.onPaymentSucceeded(mapper.readValue(payload, PaymentSucceededEvent.class)));
            case "TRAVELER:BOOKING_CONFIRMED" ->
                List.of(client.onBookingConfirmed(mapper.readValue(payload, BookingConfirmedEvent.class)));
            case "TRAVELER:PROVIDER_PAYMENT_REQUIRED" ->
                List.of(client.onProviderPaymentRequired(mapper.readValue(payload, ProviderPaymentRequiredEvent.class)));
            case "AGENT:BOOKING_REQUESTED" ->
                List.of(agent.onBookingRequested(mapper.readValue(payload, BookingRequestedEvent.class)));
            case "AGENT:QUOTE_ACCEPTED" ->
                List.of(agent.onQuoteAccepted(mapper.readValue(payload, QuoteAcceptedEvent.class)));
            case "AGENT:QUOTE_REJECTED" ->
                List.of(agent.onQuoteRejected(mapper.readValue(payload, QuoteRejectedEvent.class)));
            case "AGENT:PAYMENT_SUCCEEDED" ->
                agent.onPaymentSucceeded(mapper.readValue(payload, PaymentSucceededEvent.class));
            default -> throw new IllegalArgumentException("Unknown email job type: " + type);
        };
    }
}
