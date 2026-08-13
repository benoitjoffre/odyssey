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
}