package com.inventoryservice.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.inventoryservice.model.enums.InventoryTransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inventory_transactions")
@Getter
@Setter
@NoArgsConstructor
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 40, nullable = false)
    private InventoryTransactionType transactionType;

    @Column(
            name = "quantity_delta",
            precision = 12,
            scale = 3,
            nullable = false
    )
    private BigDecimal quantityDelta;

    @Column(
            name = "on_hand_before",
            precision = 12,
            scale = 3,
            nullable = false
    )
    private BigDecimal onHandBefore;

    @Column(
            name = "on_hand_after",
            precision = 12,
            scale = 3,
            nullable = false
    )
    private BigDecimal onHandAfter;

    @Column(
            name = "reserved_before",
            precision = 12,
            scale = 3,
            nullable = false
    )
    private BigDecimal reservedBefore;

    @Column(
            name = "reserved_after",
            precision = 12,
            scale = 3,
            nullable = false
    )
    private BigDecimal reservedAfter;

    @Column(name = "reference_type", length = 30)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "idempotency_key", length = 120, nullable = false)
    private String idempotencyKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}