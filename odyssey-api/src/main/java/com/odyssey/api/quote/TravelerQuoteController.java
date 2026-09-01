package com.odyssey.api.quote;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/travelers")
public class TravelerQuoteController {

    private final QuoteService quoteService;

    public TravelerQuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @GetMapping("/{travelerId}/quotes")
    public List<TravelerQuoteResponse> getTravelerQuotes(
        @PathVariable Long travelerId
    ) {
        return quoteService.getQuotesByTraveler(travelerId);
    }

    @PostMapping("/{travelerId}/quotes/{quoteId}/accept")
    public TravelerQuoteResponse acceptQuote(
            @PathVariable Long travelerId,
            @PathVariable Long quoteId
    ) {
        return quoteService.acceptQuote(
                quoteId,
                travelerId
        );
    }

    @PostMapping("/{travelerId}/quotes/{quoteId}/reject")
    public TravelerQuoteResponse rejectQuote(
            @PathVariable Long travelerId,
            @PathVariable Long quoteId
    ) {
        return quoteService.rejectQuote(
                quoteId,
                travelerId
        );
    }
}