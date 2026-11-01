package com.inventoryservice.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "store_inventory",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_inventory_store_product",
                        columnNames = {"store_id", "product_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class StoreInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(
            name = "on_hand_quantity",
            precision = 12,
            scale = 3,
            nullable = false
    )
    private BigDecimal onHandQuantity = BigDecimal.ZERO;

    @Column(
            name = "reserved_quantity",
            precision = 12,
            scale = 3,
            nullable = false
    )
    private BigDecimal reservedQuantity = BigDecimal.ZERO;

    @Column(
            name = "available_quantity",
            precision = 12,
            scale = 3,
            insertable = false,
            updatable = false
    )
    private BigDecimal availableQuantity;

    @Column(
            name = "low_stock_threshold",
            precision = 12,
            scale = 3,
            nullable = false
    )
    private BigDecimal lowStockThreshold = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @UpdateTimestamp
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;
}