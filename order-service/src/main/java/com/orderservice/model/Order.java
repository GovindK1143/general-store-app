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