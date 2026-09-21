package com.odyssey.api.traveler;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TravelerService {
    private final TravelerRepository travelerRepository;
    public TravelerService(TravelerRepository travelerRepository) {
        this.travelerRepository = travelerRepository;
    }
    public Traveler createTraveler(Traveler traveler) {
      return travelerRepository.save(traveler);
    }

    public List<Traveler> getTravelers() {
      return travelerRepository.findAll();
    }

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
        return travelerRepository.save(traveler);
    }
}