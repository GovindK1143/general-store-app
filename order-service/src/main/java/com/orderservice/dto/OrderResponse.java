package com.orderservice.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.orderservice.model.Order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long orderId;

    private Long userId;

    private List<OrderItemResponse> items;

    private Double totalAmount;

    private LocalDateTime orderDate;

    private String orderStatus;

    private String paymentStatus;

    public static OrderResponse fromOrder(Order order) {

        List<OrderItemResponse> itemResponses =
                order.getItems()
                        .stream()
                        .map(OrderItemResponse::fromOrderItem)
                        .collect(Collectors.toList());

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                itemResponses,
                order.getTotalAmount(),
                order.getOrderDate(),
                order.getOrderStatus(),
                order.getPaymentStatus()
        );
    }
}