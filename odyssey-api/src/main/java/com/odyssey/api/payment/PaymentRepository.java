package com.odyssey.api.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByQuoteIdOrderByCreatedAtDesc(Long quoteId);

    Optional<Payment> findFirstByQuoteIdOrderByCreatedAtDesc(Long quoteId);

    boolean existsByQuoteIdAndStatus(Long quoteId, PaymentStatus status);

    Optional<Payment> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);

    /**
     * Atomically transitions a payment to {@link PaymentStatus#PAID},
     * guarded by {@code status <> PAID} so that a Stripe webhook delivered
     * more than once (Stripe retries on non-2xx responses, and can also
     * simply send duplicates) can never mark the payment paid twice or
     * race with itself. Mirrors the compare-and-swap pattern used by
     * {@code OutboxEventRepository.updateStatusIfCurrent}.
     *
     * @return the number of rows updated (0 if the payment was already
     *         PAID, meaning this delivery is a safe-to-ignore duplicate).
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        "update Payment p " +
        "set p.status = com.odyssey.api.payment.PaymentStatus.PAID, " +
        "p.paidAt = :paidAt, " +
        "p.stripePaymentIntentId = :stripePaymentIntentId " +
        "where p.id = :id and p.status <> com.odyssey.api.payment.PaymentStatus.PAID"
    )
    int markAsPaidIfNotAlreadyPaid(
        @Param("id") Long id,
        @Param("paidAt") Instant paidAt,
        @Param("stripePaymentIntentId") String stripePaymentIntentId
    );
}
