package com.odyssey.api.provider;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/booking-requests")
public class ProviderSearchController {

    private final ProviderSearchService providerSearchService;

    public ProviderSearchController(
            ProviderSearchService providerSearchService
    ) {
        this.providerSearchService = providerSearchService;
    }

    @PreAuthorize("hasRole('AGENT')")
    @PostMapping("/{bookingRequestId}/offers/search")
    public List<? extends ProviderOffer> searchOffers(
            @PathVariable Long bookingRequestId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return providerSearchService.search(bookingRequestId, jwt.getSubject());
    }
}