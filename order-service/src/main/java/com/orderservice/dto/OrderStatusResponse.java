package com.orderservice.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusResponse {

    private Long orderId;

    private Long userId;

    private List<OrderItemResponse> items;

    private Double totalAmount;

    private LocalDateTime orderDate;

    private String orderStatus;

    private String paymentStatus;

    private String transactionId;

    private LocalDateTime paymentDate;
}