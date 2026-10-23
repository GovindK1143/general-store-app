package com.orderservice.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Saved delivery address ID at the time the order was created.
    // This is an ID from User Service, not a cross-service foreign key.
    @Column(name = "delivery_address_id")
    private Long deliveryAddressId;

    // Address snapshot: preserve the original delivery destination.
    @Column(name = "delivery_address_label", length = 30)
    private String deliveryAddressLabel;

    @Column(name = "delivery_recipient_name", length = 100)
    private String deliveryRecipientName;

    @Column(name = "delivery_mobile", length = 10)
    private String deliveryMobile;

    @Column(name = "delivery_house_flat", length = 100)
    private String deliveryHouseFlat;

    @Column(name = "delivery_street_area", length = 150)
    private String deliveryStreetArea;

    @Column(name = "delivery_city", length = 100)
    private String deliveryCity;

    @Column(name = "delivery_state", length = 100)
    private String deliveryState;

    @Column(name = "delivery_pincode", length = 6)
    private String deliveryPincode;

    @Column(name = "delivery_landmark", length = 150)
    private String deliveryLandmark;

    /*
     * One Order can contain multiple OrderItems.
     */
    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<OrderItem> items = new ArrayList<>();

    /*
     * Total amount of the complete order.
     */
    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    // Time of the most recent payment-processing attempt.
    // Null means no attempt has been recorded in this field yet.
    @Column(name = "last_payment_attempt_at")
    private LocalDateTime lastPaymentAttemptAt;

    @Column(name = "order_status", nullable = false, length = 30)
    private String orderStatus;

    @Column(name = "payment_status", nullable = false, length = 30)
    private String paymentStatus;

    /*
     * Indicates whether stock has been successfully
     * updated for all items in the order.
     */
    @Column(name = "stock_updated", nullable = false)
    private Boolean stockUpdated = false;

    /*
     * Helper method to maintain both sides
     * of the Order <-> OrderItem relationship.
     */
    public void addItem(OrderItem item) {

        items.add(item);
        item.setOrder(this);
    }

    /*
     * Helper method to remove an item safely.
     */
    public void removeItem(OrderItem item) {

        items.remove(item);
        item.setOrder(null);
    }
}