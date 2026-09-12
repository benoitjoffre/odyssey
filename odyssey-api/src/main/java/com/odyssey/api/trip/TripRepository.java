package com.odyssey.api.trip;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByTravelerIdOrderByStartDateDesc(Long travelerId);
    Optional<Trip> findByIdAndTravelerId(Long id, Long travelerId);
}