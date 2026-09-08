package com.odyssey.api.agent;

import com.odyssey.api.booking.BookingRequest;
import com.odyssey.api.need.Need;
import com.odyssey.api.need.NeedType;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.trip.Trip;

import java.time.Instant;

/**
 * Notification presentation for the Agent dashboard.
 *
 * <p>The {@code trip*}/{@code traveler*}/{@code needType} fields are not
 * duplicated data: they are resolved on read from the existing
 * {@code AgentNotification -> BookingRequest -> Need -> Trip -> Traveler}
 * relations so that the UI can group notifications by trip ({@link #tripId()})
 * without any change to how {@link AgentNotification} is persisted.</p>
 */
public record AgentNotificationResponse(
    Long id,
    String message,
    boolean read,
    Instant createdAt,
    Long bookingRequestId,
    Long tripId,
    String tripTitle,
    String tripStartDate,
    String tripEndDate,
    String travelerFirstName,
    String travelerLastName,
    NeedType needType
) {

    public static AgentNotificationResponse from(AgentNotification notification) {
        BookingRequest bookingRequest = notification.getBookingRequest();
        Need need = bookingRequest.getNeed();
        Trip trip = need.getTrip();
        Traveler traveler = trip.getTraveler();

        return new AgentNotificationResponse(
            notification.getId(),
            notification.getMessage(),
            notification.isRead(),
            notification.getCreatedAt(),
            bookingRequest.getId(),
            trip.getId(),
            trip.getTitle(),
            trip.getStartDate() != null ? trip.getStartDate().toString() : null,
            trip.getEndDate() != null ? trip.getEndDate().toString() : null,
            traveler.getFirstName(),
            traveler.getLastName(),
            need.getType()
        );
    }
}