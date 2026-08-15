package com.odyssey.api.booking;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRequestRepository
    extends JpaRepository<BookingRequest, Long> {

    Optional<BookingRequest> findByNeedId(Long needId);

    boolean existsByNeedId(Long needId);
}