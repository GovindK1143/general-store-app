package com.paymentservice.dto;

public record AdminPaymentSummary(
        long totalPayments,
        long successfulPayments,
        long pendingPayments,
        long failedPayments,
        double successfulRevenue
) {
}