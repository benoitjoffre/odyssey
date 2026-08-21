package com.odyssey.api.booking;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.odyssey.api.agent.Agent;
import com.odyssey.api.agent.AgentRepository;
import com.odyssey.api.event.BookingAssignedEvent;
import com.odyssey.api.event.BookingRequestedEvent;
import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.need.Need;
import com.odyssey.api.need.NeedRepository;
import com.odyssey.api.need.NeedStatus;
import com.odyssey.api.outbox.OutboxEvent;
import com.odyssey.api.outbox.OutboxEventRepository;
import com.odyssey.api.outbox.OutboxStatus;

import jakarta.transaction.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class BookingRequestService {

    private final BookingRequestRepository bookingRequestRepository;
    private final NeedRepository needRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final AgentRepository agentRepository;

    public BookingRequestService(
        BookingRequestRepository bookingRequestRepository,
        NeedRepository needRepository,
        OutboxEventRepository outboxEventRepository,
        ObjectMapper objectMapper,
        AgentRepository agentRepository
    ) {
        this.bookingRequestRepository = bookingRequestRepository;
        this.needRepository = needRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.agentRepository = agentRepository;
        
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
        
        
        BookingRequestedEvent event =
        new BookingRequestedEvent(
            saved.getId(),
            need.getId(),
            need.getTrip().getTraveler().getId()
        );

    try {
        String payload = objectMapper.writeValueAsString(event);

        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setEventType("BOOKING_REQUESTED");
        outboxEvent.setPayload(payload);
        outboxEvent.setStatus(OutboxStatus.PENDING);
        outboxEvent.setCreatedAt(Instant.now());

        outboxEventRepository.save(outboxEvent);

    } catch (JacksonException e) {
        throw new RuntimeException(
            "Cannot serialize booking requested event",
            e
        );
    }
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

    private BookingRequestResponse toResponse(BookingRequest bookingRequest) {
      Long assignedAgentId =
          bookingRequest.getAssignedAgent() != null
              ? bookingRequest.getAssignedAgent().getId()
              : null;

      return new BookingRequestResponse(
          bookingRequest.getId(),
          bookingRequest.getStatus(),
          bookingRequest.getNotes(),
          bookingRequest.getNeed().getId(),
          assignedAgentId
      );
    }

    @Transactional
    public BookingRequestResponse claimBookingRequest(
        Long bookingRequestId,
        Long agentId
    ) {

        BookingRequest bookingRequest = bookingRequestRepository
            .findById(bookingRequestId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Booking request not found"
                )
            );

        Agent agent = agentRepository
            .findById(agentId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Agent not found"
                )
            );

        if (bookingRequest.getAssignedAgent() != null) {
            throw new IllegalArgumentException(
                "Booking request is already assigned"
            );
        }

        bookingRequest.setAssignedAgent(agent);
        bookingRequest.setStatus(
            BookingRequestStatus.IN_PROGRESS
        );

        BookingRequest saved =
            bookingRequestRepository.save(bookingRequest);

        BookingAssignedEvent event =
            new BookingAssignedEvent(
                saved.getId(),
                saved.getNeed()
                    .getTrip()
                    .getTraveler()
                    .getId(),
                agent.getId()
            );

            try {
                String payload =
                    objectMapper.writeValueAsString(event);

                OutboxEvent outboxEvent = new OutboxEvent();

                outboxEvent.setEventType("BOOKING_ASSIGNED");
                outboxEvent.setPayload(payload);
                outboxEvent.setStatus(OutboxStatus.PENDING);
                outboxEvent.setCreatedAt(Instant.now());

                outboxEventRepository.save(outboxEvent);

            } catch (JacksonException e) {
                throw new RuntimeException(
                    "Cannot serialize booking assigned event",
                    e
                );
            }
        return toResponse(saved);
    }
}