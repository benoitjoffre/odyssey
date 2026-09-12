package com.odyssey.api.booking;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.odyssey.api.quote.QuoteService;
import com.odyssey.api.quote.TravelerQuoteResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/booking-requests")
public class BookingRequestController {

    private final BookingRequestService bookingRequestService;
    private final QuoteService quoteService;

    public BookingRequestController(
        BookingRequestService bookingRequestService,
        QuoteService quoteService
    ) {
        this.bookingRequestService = bookingRequestService;
        this.quoteService = quoteService;
    }

    @PostMapping
    public BookingRequestResponse createBookingRequest(
        @Valid @RequestBody CreateBookingRequest request
    ) {
        return bookingRequestService
            .createBookingRequest(request);
    }

    @GetMapping
    public List<BookingRequestResponse> getBookingRequests() {
        return bookingRequestService.getBookingRequests();
    }

    @GetMapping("/{id}")
    public BookingRequestResponse getBookingRequest(
        @PathVariable Long id
    ) {
        return bookingRequestService.getBookingRequest(id);
    }

    @PostMapping("/{bookingRequestId}/claim")
    public BookingRequestResponse claimBookingRequest(
        @PathVariable Long bookingRequestId,
        @RequestParam Long agentId
    ) {
        return bookingRequestService.claimBookingRequest(
            bookingRequestId,
            agentId
        );
    }

    @GetMapping("/{bookingRequestId}/quotes")
    public List<TravelerQuoteResponse> getQuotes(
        @PathVariable Long bookingRequestId
    ) {
        return quoteService.getQuotesByBookingRequest(bookingRequestId);
    }
}