package com.paymentservice.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusMessage implements Serializable {

    private Long orderId;

    private Long userId;

    private Double amount;

    private String paymentStatus;

    private String transactionId;

    private LocalDateTime paymentDate;
}