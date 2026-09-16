package com.odyssey.api.booking.confirmation;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(
        BookingService bookingService
    ) {
        this.bookingService = bookingService;
    }

    @PreAuthorize("hasRole('AGENT')")
    @PostMapping
    public BookingResponse createBooking(
        @RequestParam Long quoteId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return bookingService.createBooking(
            quoteId,
            jwt.getSubject()
        );
    }


    @PreAuthorize("hasRole('AGENT')")
    @PostMapping("/{bookingId}/provider-details")
    public BookingResponse updateProviderDetails(
        @PathVariable Long bookingId,
        @AuthenticationPrincipal Jwt jwt,
        @RequestBody UpdateProviderDetailsRequest request
    ) {
        return bookingService.updateProviderDetails(
            bookingId,
            jwt.getSubject(),
            request.providerReference(),
            request.providerPaymentUrl(),
            request.providerPaymentStatus()
        );
    }

    @PreAuthorize("hasRole('AGENT')")
    @PostMapping("/{bookingId}/confirm")
    public BookingResponse confirmBooking(
        @PathVariable Long bookingId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return bookingService.confirmBooking(
            bookingId,
            jwt.getSubject()
        );
    }
}