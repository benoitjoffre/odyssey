package com.odyssey.api.booking;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/booking-requests")
public class BookingRequestController {

    private final BookingRequestService bookingRequestService;

    public BookingRequestController(
        BookingRequestService bookingRequestService
    ) {
        this.bookingRequestService = bookingRequestService;
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
}