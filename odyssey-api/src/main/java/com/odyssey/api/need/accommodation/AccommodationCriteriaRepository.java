package com.odyssey.api.need.accommodation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccommodationCriteriaRepository
        extends JpaRepository<AccommodationCriteria, Long> {

    Optional<AccommodationCriteria> findByNeedId(Long needId);
}