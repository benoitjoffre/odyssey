package com.odyssey.api.provider.flight;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FlightSearchService {

    private final List<FlightProvider> providers;

    public FlightSearchService(List<FlightProvider> providers) {
        this.providers = providers;
    }

    public List<FlightOffer> search(FlightSearchRequest request) {

        return providers.stream()
                .flatMap(provider -> provider.search(request).stream())
                .toList();
    }
}