package com.odyssey.api.destination;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.List;

@Service
public class DestinationService {

    private final DestinationRepository destinationRepository;

    public DestinationService(DestinationRepository destinationRepository) {
        this.destinationRepository = destinationRepository;
    }

    public List<DestinationResponse> getDestinations() {
        return destinationRepository
            .findAllByOrderByCountryAscCityAsc()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public DestinationResponse createDestination(CreateDestinationRequest request) {
        String city = request.city().trim();
        String country = request.country().trim();
        String countryCode = request.countryCode().trim().toUpperCase(Locale.ROOT);

        if (city.isEmpty() || country.isEmpty()) {
            throw new IllegalArgumentException("La ville et le pays sont obligatoires.");
        }

        if (countryCode.length() != 2) {
            throw new IllegalArgumentException("Le code pays doit contenir exactement 2 lettres.");
        }

        destinationRepository
            .findByCityIgnoreCaseAndCountryCodeIgnoreCase(city, countryCode)
            .ifPresent(existing -> {
                throw new IllegalArgumentException("Cette destination existe déjà.");
            });

        Destination destination = new Destination();
        destination.setCity(city);
        destination.setCountry(country);
        destination.setCountryCode(countryCode);

        Destination savedDestination = destinationRepository.save(destination);
        return toResponse(savedDestination);
    }

    private DestinationResponse toResponse(Destination destination) {
        return new DestinationResponse(
            destination.getId(),
            destination.getCity(),
            destination.getCountry(),
            destination.getCountryCode()
        );
    }
}
