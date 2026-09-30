package com.odyssey.api.destination;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/destinations")
public class DestinationController {

    private final DestinationService destinationService;

    public DestinationController(DestinationService destinationService) {
        this.destinationService = destinationService;
    }

    @GetMapping
    public List<DestinationResponse> getDestinations() {
        return destinationService.getDestinations();
    }

    @PostMapping
    public DestinationResponse createDestination(
        @Valid @RequestBody CreateDestinationRequest request
    ) {
        return destinationService.createDestination(request);
    }
}
