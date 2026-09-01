package com.odyssey.api.provider.accommodation;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccommodationSearchService {

    private final List<AccommodationProvider> providers;

    public AccommodationSearchService(
            List<AccommodationProvider> providers
    ) {
        this.providers = providers;
    }

    public List<AccommodationOffer> search(
            AccommodationSearchRequest request
    ) {
        return providers.stream()
                .flatMap(provider ->
                        provider.search(request).stream()
                )
                .toList();
    }
}