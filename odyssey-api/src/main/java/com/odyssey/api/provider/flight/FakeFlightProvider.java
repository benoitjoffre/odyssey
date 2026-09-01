package com.odyssey.api.provider.flight;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class FakeFlightProvider implements FlightProvider {

    @Override
    public String getName() {
        return "FAKE_AIR";
    }

    @Override
    public List<FlightOffer> search(FlightSearchRequest request) {

        FlightOffer offer1 = new FlightOffer(
                getName(),
                "FA-001",
                new BigDecimal("680.00"),
                "EUR",
                request.origin(),
                request.destination(),
                request.departureDate().atTime(10, 30),
                request.departureDate().atTime(20, 15),
                "Air Odyssey"
        );

        FlightOffer offer2 = new FlightOffer(
                getName(),
                "FA-002",
                new BigDecimal("745.00"),
                "EUR",
                request.origin(),
                request.destination(),
                request.departureDate().atTime(14, 15),
                request.departureDate().atTime(23, 30),
                "Odyssey Airlines"
        );

        return List.of(offer1, offer2);
    }
}