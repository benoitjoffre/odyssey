package com.odyssey.api.quote;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/booking-requests/{bookingRequestId}/quotes")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @PostMapping
    @PreAuthorize("hasRole('AGENT')")
    public QuoteResponse createQuote(
            @PathVariable Long bookingRequestId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CreateQuoteRequest request
    ) {
        return quoteService.createQuote(
                bookingRequestId,
                jwt.getSubject(),
                request
        );
    }
}