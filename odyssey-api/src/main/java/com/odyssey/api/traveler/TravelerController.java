package com.odyssey.api.traveler;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/travelers")
public class TravelerController {

    private final TravelerService travelerService;

    public TravelerController(TravelerService travelerService) {
        this.travelerService = travelerService;
    }

    @PostMapping
    public Traveler createTraveler(@Valid @RequestBody Traveler traveler) {
        return travelerService.createTraveler(traveler);
    }

    @GetMapping
    public List<Traveler> getTravelers() {
        return travelerService.getTravelers();
    }
}