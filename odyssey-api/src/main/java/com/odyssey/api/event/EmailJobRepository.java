package com.odyssey.api.event;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface EmailJobRepository extends JpaRepository<EmailJob, Long> {

    boolean existsByDeduplicationKey(String key);

    @Query("select j.id from EmailJob j where j.status = 'PENDING' and j.nextAttemptAt <= :now order by j.id")
    List<Long> findReady(@Param("now") Instant now, Pageable pageable);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update EmailJob j set j.status = 'PROCESSING', j.attempts = j.attempts + 1, "
        + "j.startedAt = :now, j.firstAttemptAt = coalesce(j.firstAttemptAt, :now), j.leaseToken = :token "
        + "where j.id = :id and j.status = 'PENDING' and j.nextAttemptAt <= :now")
    int claim(@Param("id") Long id, @Param("now") Instant now, @Param("token") String token);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update EmailJob j set j.status = 'COMPLETED', j.completedAt = :now, "
        + "j.providerId = :providerId, j.lastError = null where j.id = :id "
        + "and j.status = 'PROCESSING' and j.leaseToken = :token")
    int complete(@Param("id") Long id, @Param("token") String token,
        @Param("now") Instant now, @Param("providerId") String providerId);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update EmailJob j set j.status = :status, j.nextAttemptAt = :next, j.lastError = :error "
        + "where j.id = :id and j.status = 'PROCESSING' and j.leaseToken = :token")
    int fail(@Param("id") Long id, @Param("token") String token, @Param("status") EmailJob.Status status,
        @Param("next") Instant next, @Param("error") String error);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update EmailJob j set j.status = 'PENDING', j.nextAttemptAt = :now, "
        + "j.lastError = 'Processing lease expired; retry pending' "
        + "where j.status = 'PROCESSING' and j.startedAt < :cutoff and j.attempts < 8")
    int recover(@Param("cutoff") Instant cutoff, @Param("now") Instant now);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update EmailJob j set j.status = 'FAILED', j.lastError = 'Processing lease expired after final attempt' "
        + "where j.status = 'PROCESSING' and j.startedAt < :cutoff and j.attempts >= 8")
    int failExpired(@Param("cutoff") Instant cutoff);
}
