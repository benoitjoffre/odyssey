package com.odyssey.api.travelevent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TravelEventRepository
        extends JpaRepository<TravelEvent, Long> {

    List<TravelEvent> findByExperienceId(Long experienceId);
}