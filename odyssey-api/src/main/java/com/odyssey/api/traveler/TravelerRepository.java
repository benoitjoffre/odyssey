package com.odyssey.api.traveler;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface TravelerRepository extends JpaRepository<Traveler, Long> {

    Optional<Traveler> findByAuth0Subject(String auth0Subject);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Traveler t where t.auth0Subject = :subject")
    Optional<Traveler> findForOnboarding(@Param("subject") String auth0Subject);

    Optional<Traveler> findByEmail(String email);
}