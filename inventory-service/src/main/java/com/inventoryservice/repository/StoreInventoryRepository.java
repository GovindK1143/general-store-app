
package com.inventoryservice.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.inventoryservice.model.StoreInventory;

@Repository
public interface StoreInventoryRepository
        extends JpaRepository<StoreInventory, Long> {

    Optional<StoreInventory> findByStoreIdAndProductId(
            Long storeId,
            Long productId
    );

    boolean existsByStoreIdAndProductId(
            Long storeId,
            Long productId
    );

    List<StoreInventory> findByStoreIdOrderByProductIdAsc(
            Long storeId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT inventory
            FROM StoreInventory inventory
            WHERE inventory.storeId = :storeId
              AND inventory.productId = :productId
            """)
    Optional<StoreInventory> findByStoreIdAndProductIdForUpdate(
            @Param("storeId") Long storeId,
            @Param("productId") Long productId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT inventory
            FROM StoreInventory inventory
            WHERE inventory.storeId = :storeId
              AND inventory.productId IN :productIds
            ORDER BY inventory.productId ASC
            """)
    List<StoreInventory> findAllByStoreIdAndProductIdsForUpdate(
            @Param("storeId") Long storeId,
            @Param("productIds") Collection<Long> productIds
    );

    @Modifying
    @Query(
            value = """
                INSERT INTO store_inventory (
                    store_id,
                    product_id,
                    on_hand_quantity,
                    reserved_quantity,
                    low_stock_threshold,
                    version,
                    last_updated
                )
                VALUES (
                    :storeId,
                    :productId,
                    0.000,
                    0.000,
                    :lowStockThreshold,
                    0,
                    CURRENT_TIMESTAMP(6)
                )
                ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)
                """,
            nativeQuery = true
    )
    int insertIfAbsent(
            @Param("storeId") Long storeId,
            @Param("productId") Long productId,
            @Param("lowStockThreshold") BigDecimal lowStockThreshold
    );
}
