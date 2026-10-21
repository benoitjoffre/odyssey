package com.odyssey.api.booking.confirmation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;



public interface BookingRepository
        extends JpaRepository<Booking, Long> {

          
  Optional<Booking> findByQuoteId(Long quoteId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select b from Booking b where b.id = :id")
  Optional<Booking> findByIdForUpdate(@Param("id") Long id);

  Optional<Booking> findByQuoteBookingRequestId(Long bookingRequestId);

  boolean existsByQuoteId(Long quoteId);
}