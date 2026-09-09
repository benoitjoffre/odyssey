package com.odyssey.api.quote;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuoteRepository extends JpaRepository<Quote, Long> {

    List<Quote> findByBookingRequestId(Long bookingRequestId);

    List<Quote> findByBookingRequestNeedTripTravelerIdAndStatusNot(
        Long travelerId,
        QuoteStatus status
    );

    /**
     * Locks this Quote row ({@code SELECT ... FOR UPDATE}) for the
     * duration of the current transaction. Used by
     * {@code PaymentService.createCheckoutSession} to serialize concurrent
     * Checkout Session creation attempts for the same Quote: a second
     * transaction calling this method for the same {@code id} blocks until
     * the first transaction commits or rolls back, so only one transaction
     * at a time can decide whether to create or reuse this Quote's
     * Payment.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Quote q where q.id = :id")
    Optional<Quote> findByIdForUpdate(@Param("id") Long id);
}