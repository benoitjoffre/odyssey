package com.odyssey.api.quote;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/travelers")
public class TravelerQuoteController {

    private final QuoteService quoteService;

    public TravelerQuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @PreAuthorize("hasRole('TRAVELER')")
    @GetMapping("/me/quotes")
    public List<TravelerQuoteResponse> getTravelerQuotes(
        @AuthenticationPrincipal Jwt jwt
    ) {
        return quoteService.getQuotesByCurrentTraveler(jwt.getSubject());
    }

    @PreAuthorize("hasRole('TRAVELER')")
    @PostMapping("/me/quotes/{quoteId}/accept")
    public TravelerQuoteResponse acceptQuote(
            @PathVariable Long quoteId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return quoteService.acceptQuote(quoteId, jwt.getSubject());
    }

    @PreAuthorize("hasRole('TRAVELER')")
    @PostMapping("/me/quotes/{quoteId}/reject")
    public TravelerQuoteResponse rejectQuote(
            @PathVariable Long quoteId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return quoteService.rejectQuote(quoteId, jwt.getSubject());
    }
}