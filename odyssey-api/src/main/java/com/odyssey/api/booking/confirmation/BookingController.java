package com.odyssey.api.booking.confirmation;

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

    @PostMapping
    public BookingResponse createBooking(
        @RequestParam Long quoteId,
        @RequestParam Long agentId
    ) {
        return bookingService.createBooking(
            quoteId,
            agentId
        );
    }


    @PostMapping("/{bookingId}/confirm")
    public BookingResponse confirmBooking(
        @PathVariable Long bookingId,
        @RequestParam Long agentId
    ) {
        return bookingService.confirmBooking(
            bookingId,
            agentId
        );
    }
}