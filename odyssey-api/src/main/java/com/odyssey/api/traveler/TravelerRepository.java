package com.odyssey.api.traveler;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelerRepository extends JpaRepository<Traveler, Long> {

    Optional<Traveler> findByAuth0Subject(String auth0Subject);

    Optional<Traveler> findByEmail(String email);
}