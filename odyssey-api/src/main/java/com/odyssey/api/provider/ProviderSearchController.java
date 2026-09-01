package com.odyssey.api.provider;

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

    @PostMapping("/{bookingRequestId}/offers/search")
    public List<? extends ProviderOffer> searchOffers(
            @PathVariable Long bookingRequestId
    ) {
        return providerSearchService.search(bookingRequestId);
    }
}