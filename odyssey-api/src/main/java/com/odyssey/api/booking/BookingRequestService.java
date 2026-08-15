package com.odyssey.api.booking;

import java.util.List;

import org.springframework.stereotype.Service;

import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.need.Need;
import com.odyssey.api.need.NeedRepository;
import com.odyssey.api.need.NeedStatus;

@Service
public class BookingRequestService {

    private final BookingRequestRepository bookingRequestRepository;
    private final NeedRepository needRepository;

    public BookingRequestService(
        BookingRequestRepository bookingRequestRepository,
        NeedRepository needRepository
    ) {
        this.bookingRequestRepository = bookingRequestRepository;
        this.needRepository = needRepository;
    }

    public BookingRequestResponse createBookingRequest(
        CreateBookingRequest request
    ) {
        Need need = needRepository
            .findById(request.needId())
            .orElseThrow(() ->
                new ResourceNotFoundException("Need not found")
            );

        if (bookingRequestRepository.existsByNeedId(need.getId())) {
            throw new IllegalArgumentException(
                "A booking request already exists for this need"
            );
        }

        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setNeed(need);
        bookingRequest.setNotes(request.notes());
        bookingRequest.setStatus(BookingRequestStatus.REQUESTED);

        BookingRequest saved = bookingRequestRepository.save(bookingRequest);

        need.setStatus(NeedStatus.REQUESTED);
        needRepository.save(need);

        return toResponse(saved);
    }

    public BookingRequestResponse getBookingRequest(Long id) {
        BookingRequest bookingRequest = bookingRequestRepository
            .findById(id)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Booking request not found"
                )
            );

        return toResponse(bookingRequest);
    }

    public List<BookingRequestResponse> getBookingRequests() {
        return bookingRequestRepository.findAll()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private BookingRequestResponse toResponse(
        BookingRequest bookingRequest
    ) {
        return new BookingRequestResponse(
            bookingRequest.getId(),
            bookingRequest.getStatus(),
            bookingRequest.getNotes(),
            bookingRequest.getNeed().getId()
        );
    }
}