package com.odyssey.api.quote;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/booking-requests/{bookingRequestId}/quotes")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @PostMapping
    public QuoteResponse createQuote(
            @PathVariable Long bookingRequestId,
            @RequestParam Long agentId,
            @RequestBody CreateQuoteRequest request
    ) {
        return quoteService.createQuote(
                bookingRequestId,
                agentId,
                request
        );
    }

    @PostMapping("/{quoteId}/send")
    public QuoteResponse sendQuote(
            @PathVariable Long quoteId,
            @RequestParam Long agentId
    ) {
        return quoteService.sendQuote(quoteId, agentId);
    }
}