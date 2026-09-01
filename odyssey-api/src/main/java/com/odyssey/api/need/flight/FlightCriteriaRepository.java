package com.odyssey.api.need.flight;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FlightCriteriaRepository
        extends JpaRepository<FlightCriteria, Long> {

    Optional<FlightCriteria> findByNeedId(Long needId);
}