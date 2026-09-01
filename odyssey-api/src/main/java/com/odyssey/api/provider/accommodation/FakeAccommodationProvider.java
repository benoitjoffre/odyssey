package com.odyssey.api.provider.accommodation;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class FakeAccommodationProvider implements AccommodationProvider {

    @Override
    public String getName() {
        return "FAKE_HOTELS";
    }

    @Override
    public List<AccommodationOffer> search(
            AccommodationSearchRequest request
    ) {

        AccommodationOffer offer1 = new AccommodationOffer(
                getName(),
                "HOTEL-001",
                new BigDecimal("850.00"),
                "EUR",
                "Hotel Nacional",
                request.city(),
                request.checkIn(),
                request.checkOut(),
                "Chambre double"
        );

        AccommodationOffer offer2 = new AccommodationOffer(
                getName(),
                "HOTEL-002",
                new BigDecimal("620.00"),
                "EUR",
                "Casa Odyssey",
                request.city(),
                request.checkIn(),
                request.checkOut(),
                "Chambre privée"
        );

        return List.of(offer1, offer2);
    }
}