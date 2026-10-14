
package com.cartservice.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import com.cartservice.model.CartCheckoutSnapshot;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface CartCheckoutSnapshotRepository
        extends JpaRepository<CartCheckoutSnapshot, Long> {

    /*
     * Create a placeholder only if this order does not already
     * have a snapshot. This is a MySQL-specific atomic operation.
     *
     * If the payment event arrives first, paymentSucceeded is true.
     * If checkout arrives first, it is false.
     */
    @Modifying
    @Transactional
    @Query(
            value = """
                INSERT IGNORE INTO cart_checkout_snapshots
                    (order_id, user_id, processed, payment_succeeded,
                     snapshot_ready, created_at)
                VALUES
                    (:orderId, :userId, false, :paymentSucceeded,
                     false, :createdAt)
                """,
            nativeQuery = true
    )
    int claimSnapshot(
            @Param("orderId") Long orderId,
            @Param("userId") Long userId,
            @Param("paymentSucceeded") boolean paymentSucceeded,
            @Param("createdAt") LocalDateTime createdAt
    );

    /*
     * Lock an existing snapshot while checkout completion
     * and payment-event processing coordinate their updates.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM CartCheckoutSnapshot s
            WHERE s.orderId = :orderId
            """)
    Optional<CartCheckoutSnapshot> findByOrderIdForUpdate(
            @Param("orderId") Long orderId
    );
}
