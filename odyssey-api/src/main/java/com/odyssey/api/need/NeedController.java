package com.odyssey.api.need;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/needs")
public class NeedController {

    private final NeedService needService;

    public NeedController(NeedService needService) {
        this.needService = needService;
    }

    @PreAuthorize("hasRole('TRAVELER')")
    @PostMapping
    public NeedResponse createNeed(
        @Valid @RequestBody CreateNeedRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return needService.createNeed(request, jwt.getSubject());
    }

    @PreAuthorize("hasRole('TRAVELER')")
    @GetMapping
    public List<NeedResponse> getNeeds(@AuthenticationPrincipal Jwt jwt) {
        return needService.getNeeds(jwt.getSubject());
    }

    @PreAuthorize("hasRole('TRAVELER')")
    @GetMapping("/{id}")
    public NeedResponse getNeed(
        @PathVariable Long id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return needService.getNeed(id, jwt.getSubject());
    }

    @PreAuthorize("hasRole('TRAVELER')")
    @GetMapping("/trip/{tripId}")
    public List<NeedResponse> getNeedsByTrip(
        @PathVariable Long tripId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return needService.getNeedsByTrip(tripId, jwt.getSubject());
    }

    @PreAuthorize("hasRole('TRAVELER')")
    @PatchMapping("/{id}/notes")
    public NeedResponse updateNeedNotes(
        @PathVariable Long id,
        @Valid @RequestBody UpdateNeedNotesRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return needService.updateNotes(id, request, jwt.getSubject());
    }
}