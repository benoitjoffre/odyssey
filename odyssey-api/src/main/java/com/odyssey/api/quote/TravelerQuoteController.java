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
}