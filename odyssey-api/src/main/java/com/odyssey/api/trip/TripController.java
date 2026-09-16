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
    private final com.odyssey.api.quote.QuoteService quoteService;

    public TripController(
        TripService tripService,
        com.odyssey.api.quote.QuoteService quoteService
    ) {
        this.tripService = tripService;
        this.quoteService = quoteService;
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

    @PatchMapping("/{tripId}/assistance-fee")
    @PreAuthorize("hasRole('AGENT')")
    public TripResponse updateAssistanceFee(
        @PathVariable Long tripId,
        @Valid @RequestBody UpdateAssistanceFeeRequest request
    ) {
        return tripService.updateAssistanceFee(tripId, request.assistanceFee());
    }

    @PostMapping("/{tripId}/quotes/send")
    @PreAuthorize("hasRole('AGENT')")
    public SendTripQuotesResponse sendQuotes(
        @PathVariable Long tripId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return quoteService.sendDraftQuotesForTrip(tripId, jwt.getSubject());
    }
}