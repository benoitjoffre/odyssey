package com.odyssey.api.booking.confirmation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookingRepository
        extends JpaRepository<Booking, Long> {

    Optional<Booking> findByQuoteId(Long quoteId);

    boolean existsByQuoteId(Long quoteId);
}