package com.odyssey.api.provider.flight;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/provider/flights")
public class FlightSearchController {

    private final FlightSearchService flightSearchService;

    public FlightSearchController(FlightSearchService flightSearchService) {
        this.flightSearchService = flightSearchService;
    }

    @PostMapping("/search")
    public List<FlightOffer> search(
            @RequestBody FlightSearchRequest request
    ) {
        return flightSearchService.search(request);
    }
}