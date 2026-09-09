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

    /**
     * Atomically transitions a payment to {@link PaymentStatus#FAILED},
     * guarded by {@code status = PENDING} so this can NEVER downgrade a
     * PAID payment (a late {@code checkout.session.expired}/
     * {@code checkout.session.async_payment_failed} webhook must not undo a
     * successful payment) and is idempotent for an already-FAILED payment
     * (a duplicate/retried failure webhook is a safe no-op).
     *
     * @return the number of rows updated (0 if the payment was not
     *         PENDING, meaning this delivery must not change its state).
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        "update Payment p " +
        "set p.status = com.odyssey.api.payment.PaymentStatus.FAILED " +
        "where p.id = :id and p.status = com.odyssey.api.payment.PaymentStatus.PENDING"
    )
    int markAsFailedIfPending(@Param("id") Long id);
}
