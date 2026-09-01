package com.odyssey.api.booking.confirmation;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FakeBookingProvider {

    public String confirmBooking(Booking booking) {

        // Simulation d'un appel à une API fournisseur
        return "CONF-" +
            UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }
}