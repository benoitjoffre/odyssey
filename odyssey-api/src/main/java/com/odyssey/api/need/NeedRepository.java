package com.odyssey.api.need;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NeedRepository extends JpaRepository<Need, Long> {

    List<Need> findByTripId(Long tripId);
}