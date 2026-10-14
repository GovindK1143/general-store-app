package com.cartservice.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cart_checkout_snapshots")
public class CartCheckoutSnapshot {

    /*
     * Order ID is the primary key.
     * One order can have only one checkout snapshot.
     */
    @Id
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /*
     * True once the purchased quantities have been
     * removed from the cart successfully.
     */
    @Column(name = "processed", nullable = false)
    private Boolean processed = false;

    @Column(name = "payment_succeeded", nullable = false)
    private Boolean paymentSucceeded = false;

    @Column(name = "snapshot_ready", nullable = false)
    private Boolean snapshotReady = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}