package com.odyssey.api.traveler;
import org.springframework.stereotype.Service;
import java.time.Instant;
import tools.jackson.databind.ObjectMapper;

import com.odyssey.api.event.TravelerOnboardingCompletedEvent;
import com.odyssey.api.outbox.OutboxEvent;
import com.odyssey.api.outbox.OutboxEventRepository;
import com.odyssey.api.outbox.OutboxStatus;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TravelerService {
    private final TravelerRepository travelerRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    
    public TravelerService(TravelerRepository travelerRepository, OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.travelerRepository = travelerRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }
    public Traveler createTraveler(Traveler traveler) {
      return travelerRepository.save(traveler);
    }

    public List<Traveler> getTravelers() {
      return travelerRepository.findAll();
    }

    @Transactional
    public Traveler completeOnboarding(
    String auth0Subject,
    TravelerOnboardingRequest request
) {
        Traveler traveler = travelerRepository.findByAuth0Subject(auth0Subject)
                .orElseThrow(() -> new IllegalArgumentException("Traveler not found"));
        traveler.setFirstName(request.firstName());
        traveler.setLastName(request.lastName());
        traveler.setPhoneNumber(request.phoneNumber());
        traveler.setWhatsappNumber(request.whatsappNumber());
        traveler.setPreferredLanguage(request.preferredLanguage());
        traveler.setOnboardingCompleted(true);

        // Publish an outbox event indicating that the traveler has completed onboarding
        TravelerOnboardingCompletedEvent event = new TravelerOnboardingCompletedEvent(traveler.getId());
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setEventType("TRAVELER_ONBOARDING_COMPLETED");
        outboxEvent.setPayload(objectMapper.writeValueAsString(event));
        outboxEvent.setStatus(OutboxStatus.PENDING);
        outboxEvent.setCreatedAt(Instant.now());

        // Save the outbox event to the repository
        outboxEventRepository.save(outboxEvent);
        
        return travelerRepository.save(traveler);
    }
}