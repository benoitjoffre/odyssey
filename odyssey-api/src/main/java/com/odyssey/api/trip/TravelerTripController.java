package com.odyssey.api.trip;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/travelers")
public class TravelerTripController {

    private final TripService tripService;

    public TravelerTripController(
        TripService tripService
    ) {
        this.tripService = tripService;
    }

    @GetMapping("/{travelerId}/trips")
    public List<TripResponse> getTravelerTrips(
        @PathVariable Long travelerId
    ) {
        return tripService.getTripsByTraveler(travelerId);
    }
}