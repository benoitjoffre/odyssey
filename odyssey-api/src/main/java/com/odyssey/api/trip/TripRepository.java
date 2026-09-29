package com.odyssey.api.trip;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;

import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByTravelerIdOrderByStartDateDesc(Long travelerId);
    Optional<Trip> findByIdAndTravelerId(Long id, Long travelerId);
    boolean existsByTravelEventId(Long travelEventId);

    @Modifying
    @Query("update Trip t set t.travelEvent = null where t.travelEvent.id = :travelEventId")
    int clearTravelEventByTravelEventId(@Param("travelEventId") Long travelEventId);

    @Modifying
    @Query("update Trip t set t.travelEvent = null where t.travelEvent.id in :travelEventIds")
    int clearTravelEventByTravelEventIds(@Param("travelEventIds") List<Long> travelEventIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trip t where t.id = :id")
    Optional<Trip> findByIdForUpdate(@Param("id") Long id);
}