package com.odyssey.api.travelevent;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/travel-events")
public class TravelEventController {

    private final TravelEventService travelEventService;

    public TravelEventController(
        TravelEventService travelEventService
    ) {
        this.travelEventService = travelEventService;
    }

    @PostMapping
    public TravelEventResponse create(
        @Valid @RequestBody CreateTravelEventRequest request
    ) {
        return travelEventService.create(request);
    }

    @GetMapping
    public List<TravelEventResponse> getAll() {
        return travelEventService.getAll();
    }

    @GetMapping("/{id}")
    public TravelEventResponse getById(@PathVariable Long id) {
        return travelEventService.getById(id);
    }
}