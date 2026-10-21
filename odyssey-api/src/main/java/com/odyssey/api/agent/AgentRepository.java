package com.odyssey.api.agent;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AgentRepository extends JpaRepository<Agent, Long> {

    List<Agent> findByStatus(AgentStatus status);

    Optional<Agent> findByAuth0Subject(String auth0Subject);

    Optional<Agent> findByEmail(String email);

    @Query("""
        select a from Agent a
        where locate(lower(:search), lower(concat(coalesce(a.firstName, ''), ' ', coalesce(a.lastName, '')))) > 0
           or locate(lower(:search), lower(a.email)) > 0
        """)
    Page<Agent> searchProfiles(@Param("search") String search, Pageable pageable);
}