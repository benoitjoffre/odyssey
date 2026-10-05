package com.odyssey.api.event;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerNotification;
import com.odyssey.api.traveler.TravelerNotificationRepository;
import com.odyssey.api.traveler.TravelerNotificationResponse;
import com.odyssey.api.traveler.TravelerNotificationSseService;
import com.odyssey.api.traveler.TravelerRepository;

@Component
public class TravelerNotificationEventListener {

    private static final Logger logger =
        LoggerFactory.getLogger(TravelerNotificationEventListener.class);

    private final TravelerRepository travelerRepository;
    private final TravelerNotificationRepository notificationRepository;
    private final TravelerNotificationSseService sseService;

    public TravelerNotificationEventListener(
        TravelerRepository travelerRepository,
        TravelerNotificationRepository notificationRepository,
        TravelerNotificationSseService sseService
    ) {
        this.travelerRepository = travelerRepository;
        this.notificationRepository = notificationRepository;
        this.sseService = sseService;
    }

    @EventListener
    public void onTripQuotesSent(TripQuotesSentEvent event) {
        try {
            handleTripQuotesSent(event);
        } catch (Exception exception) {
            logger.error(
                "Failed to handle TripQuotesSentEvent for traveler {}",
                event.travelerId(),
                exception
            );
        }
    }

    private void handleTripQuotesSent(TripQuotesSentEvent event) {
        Traveler traveler = travelerRepository
            .findById(event.travelerId())
            .orElseThrow(() ->
                new ResourceNotFoundException("Traveler not found")
            );

        TravelerNotification notification = new TravelerNotification();
        notification.setTraveler(traveler);
        notification.setType("QUOTE_AVAILABLE");
        notification.setTitle("Nouvelle proposition disponible");
        notification.setMessage(
            "Votre conseiller a préparé une nouvelle proposition pour votre voyage."
        );
        notification.setTargetUrl("/traveler/quotes");
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());

        TravelerNotification savedNotification = notificationRepository.save(notification);
        TravelerNotificationResponse response =
            TravelerNotificationResponse.from(savedNotification);

        sseService.send(traveler.getId(), response);

        logger.info(
            "Traveler notification created for traveler {} and sent over SSE",
            traveler.getId()
        );
    }
}
