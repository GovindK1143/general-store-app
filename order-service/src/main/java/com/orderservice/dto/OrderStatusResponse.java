package com.orderservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusResponse {
    private Long orderId;
    private Long userId;
    private Long productId;
    private int quantity;
    private double totalPrice;
    private LocalDateTime orderDate;
    private String paymentStatus;
    private String transactionId;
    private LocalDateTime paymentDate;
}

