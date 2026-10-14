package com.orderservice.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.orderservice.client.PaymentServiceClient;
import com.orderservice.model.Order;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PaymentProcessingService {

    private static final String PAYMENT_SERVICE_CB =
            "paymentServiceCB";

    private static final String PAYMENT_PENDING = "PENDING";

    @Autowired
    private PaymentServiceClient paymentServiceClient;

    // =========================================================
    // PROCESS PAYMENT THROUGH PAYMENT SERVICE
    // =========================================================

    @CircuitBreaker(
            name = PAYMENT_SERVICE_CB,
            fallbackMethod = "paymentServiceFallback"
    )
    public Map<String, Object> processPayment(Order order) {

        if (order == null || order.getId() == null) {
            throw new IllegalArgumentException(
                    "A valid order is required for payment processing"
            );
        }

        Map<String, Object> paymentRequest = new HashMap<>();

        paymentRequest.put("orderId", order.getId());
        paymentRequest.put("userId", order.getUserId());
        paymentRequest.put("amount", order.getTotalAmount());

        log.info(
                "Calling Payment Service. orderId={}, amount={}",
                order.getId(),
                order.getTotalAmount()
        );

        Map<String, Object> paymentResponse =
                paymentServiceClient.processPayment(paymentRequest);

        if (paymentResponse == null) {
            throw new IllegalStateException(
                    "Payment Service returned an empty response"
            );
        }

        return paymentResponse;
    }

    // =========================================================
    // PAYMENT SERVICE FALLBACK
    // =========================================================

    public Map<String, Object> paymentServiceFallback(
            Order order,
            Exception exception) {

        log.warn(
                "Payment Service unavailable for orderId={}. " +
                        "Keeping payment PENDING. Reason: {}",
                order != null ? order.getId() : null,
                exception.getMessage()
        );

        Map<String, Object> response = new HashMap<>();

        if (order != null) {
            response.put("orderId", order.getId());
        }

        response.put("paymentStatus", PAYMENT_PENDING);
        response.put(
                "message",
                "Payment Service is unavailable. Payment remains pending."
        );

        return response;
    }
}
