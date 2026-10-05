package com.odyssey.api.traveler;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelerNotificationRepository extends JpaRepository<TravelerNotification, Long> {

    List<TravelerNotification> findByTravelerIdOrderByCreatedAtDesc(Long travelerId);
}
