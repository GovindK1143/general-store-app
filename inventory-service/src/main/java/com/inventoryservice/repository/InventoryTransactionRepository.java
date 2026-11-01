
package com.inventoryservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.inventoryservice.model.InventoryTransaction;

@Repository
public interface InventoryTransactionRepository
        extends JpaRepository<InventoryTransaction, Long> {

    Optional<InventoryTransaction> findByIdempotencyKey(
            String idempotencyKey
    );

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<InventoryTransaction>
    findByStoreIdAndProductIdOrderByCreatedAtDesc(
            Long storeId,
            Long productId
    );

    List<InventoryTransaction>
    findByStoreIdOrderByCreatedAtDesc(Long storeId);
}
