package com.odyssey.api.need;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/needs")
public class NeedController {

    private final NeedService needService;

    public NeedController(NeedService needService) {
        this.needService = needService;
    }

    @PostMapping
    public NeedResponse createNeed(
        @Valid @RequestBody CreateNeedRequest request
    ) {
        return needService.createNeed(request);
    }

    @GetMapping
    public List<NeedResponse> getNeeds() {
        return needService.getNeeds();
    }

    @GetMapping("/{id}")
    public NeedResponse getNeed(@PathVariable Long id) {
        return needService.getNeed(id);
    }

    @GetMapping("/trip/{tripId}")
    public List<NeedResponse> getNeedsByTrip(
        @PathVariable Long tripId
    ) {
        return needService.getNeedsByTrip(tripId);
    }
}