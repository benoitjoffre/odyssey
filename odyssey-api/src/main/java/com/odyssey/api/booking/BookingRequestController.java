package com.odyssey.api.booking;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.odyssey.api.quote.AgentQuoteResponse;
import com.odyssey.api.quote.QuoteService;

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

    @PreAuthorize("hasRole('AGENT')")
    @GetMapping
    public List<BookingRequestResponse> getBookingRequests() {
        return bookingRequestService.getBookingRequests();
    }

    @PreAuthorize("hasRole('AGENT')")
    @GetMapping("/{id}")
    public BookingRequestResponse getBookingRequest(
        @PathVariable Long id
    ) {
        return bookingRequestService.getBookingRequest(id);
    }

    @PreAuthorize("hasRole('AGENT')")
    @PostMapping("/{bookingRequestId}/claim")
    public BookingRequestResponse claimBookingRequest(
        @PathVariable Long bookingRequestId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return bookingRequestService.claimBookingRequest(
            bookingRequestId,
            jwt.getSubject()
        );
    }

    @PreAuthorize("hasRole('AGENT')")
    @GetMapping("/{bookingRequestId}/quotes")
    public List<AgentQuoteResponse> getQuotes(
        @PathVariable Long bookingRequestId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return quoteService.getQuotesByBookingRequest(
            bookingRequestId,
            jwt.getSubject()
        );
    }
}