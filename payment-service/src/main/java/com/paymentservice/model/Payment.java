package com.paymentservice.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    private double amount;

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus;

    private String transactionId;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;
}
