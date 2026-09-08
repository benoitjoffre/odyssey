package com.odyssey.api.outbox;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface OutboxEventRepository
    extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatus(OutboxStatus status);

    /**
     * Atomically transitions an event from {@code expectedStatus} to
     * {@code newStatus}. Used as a compare-and-swap so that two concurrent
     * processing runs can never both "claim" the same pending event.
     *
     * <p>Explicitly {@code @Transactional}: this method must always run
     * inside its own transaction (so that the mandatory {@code flush()}
     * triggered by {@code flushAutomatically = true} has an EntityManager
     * transaction to work with), whether it is called from within an
     * existing transaction (e.g. a {@code @Transactional} test) or from a
     * non-transactional caller such as the {@code @Scheduled} background
     * thread in {@link OutboxProcessor}.</p>
     *
     * @return the number of rows updated (0 if another execution already
     *         moved the event out of {@code expectedStatus}).
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        "update OutboxEvent e " +
        "set e.status = :newStatus " +
        "where e.id = :id and e.status = :expectedStatus"
    )
    int updateStatusIfCurrent(
        @Param("id") Long id,
        @Param("expectedStatus") OutboxStatus expectedStatus,
        @Param("newStatus") OutboxStatus newStatus
    );
}