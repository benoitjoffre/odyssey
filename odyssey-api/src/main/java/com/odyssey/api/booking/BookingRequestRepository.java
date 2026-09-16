package com.odyssey.api.booking;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRequestRepository
    extends JpaRepository<BookingRequest, Long> {

    Optional<BookingRequest> findByNeedId(Long needId);

    List<BookingRequest> findByNeedTripId(Long tripId);

    boolean existsByNeedId(Long needId);

}