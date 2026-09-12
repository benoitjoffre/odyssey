package com.odyssey.api.trip;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PreAuthorize("hasRole('TRAVELER')")
    @PostMapping
    public TripResponse createTrip(@Valid @RequestBody CreateTripRequest request, @AuthenticationPrincipal Jwt jwt) {
        return tripService.createTrip(request, jwt.getSubject());
    }

    @PreAuthorize("hasRole('TRAVELER')")
    @GetMapping
    public List<TripResponse> getTrips( @AuthenticationPrincipal Jwt jwt) {
        String auth0Subject = jwt.getSubject();
        return tripService.getTrips(auth0Subject);
    }

    @PreAuthorize("hasRole('TRAVELER')")
    @GetMapping("/{id}")
    public TripResponse getTrip(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return tripService.getTrip(id, jwt.getSubject());
    }

    @PreAuthorize("hasRole('TRAVELER')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTrip(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        tripService.deleteTrip(id, jwt.getSubject());
    }

    @PreAuthorize("hasRole('TRAVELER')")
    @GetMapping("/{id}/detail")
    public TripDetailResponse getTripDetail(
        @PathVariable Long id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return tripService.getTripDetail(id, jwt.getSubject());
    }
}