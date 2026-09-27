package com.productservice.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.productservice.model.StockUpdateTransaction;

@Repository
public interface StockUpdateTransactionRepository
        extends JpaRepository<StockUpdateTransaction, Long> {

    Optional<StockUpdateTransaction> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    @Modifying
    @Query(
            value = """
                INSERT IGNORE INTO stock_update_transactions
                (order_id, status, processed_at)
                VALUES (:orderId, 'PROCESSING', :processedAt)
                """,
            nativeQuery = true
    )
    int claimOrder(
            @Param("orderId") Long orderId,
            @Param("processedAt") LocalDateTime processedAt
    );
}