package com.orderservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusMessage {
    private Long orderId;
    private String paymentStatus;
    private String transactionId;
    private double amount;
}
