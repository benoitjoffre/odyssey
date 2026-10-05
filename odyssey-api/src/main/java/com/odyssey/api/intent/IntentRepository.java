package com.odyssey.api.intent;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IntentRepository extends JpaRepository<Intent, Long> {

	List<Intent> findByTravelerIdOrderByIdDesc(Long travelerId);

	Optional<Intent> findByIdAndTravelerId(Long id, Long travelerId);
}