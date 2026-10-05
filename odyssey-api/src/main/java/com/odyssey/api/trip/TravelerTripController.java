package com.odyssey.api.trip;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

    @PreAuthorize("hasRole('TRAVELER')")
    @GetMapping("/{travelerId}/trips")
    public List<TripResponse> getTravelerTrips(
        @PathVariable Long travelerId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return tripService.getOwnedTripsByTravelerId(travelerId, jwt.getSubject());
    }
}