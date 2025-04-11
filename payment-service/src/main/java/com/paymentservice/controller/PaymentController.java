package com.paymentservice.controller;

import java.util.HashMap;
import java.util.Map;

import org.apache.hc.core5.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.paymentservice.model.Payment;
import com.paymentservice.service.PaymentService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/payments")
@Slf4j
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody Payment payment) {
        log.info("🔹 Processing payment for Order ID: {}", payment.getOrderId());
        Payment savedPayment = paymentService.processPayment(payment);

        Map<String, Object> response = new HashMap<>();
        response.put("id", savedPayment.getId());
        response.put("orderId", savedPayment.getOrderId());
        response.put("paymentStatus", savedPayment.getPaymentStatus());
        response.put("transactionId", savedPayment.getTransactionId());
        response.put("paymentDate", savedPayment.getPaymentDate());
        response.put("amount", savedPayment.getAmount());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/createPending")
    public ResponseEntity<Payment> createPendingPayment(@RequestBody Payment payment) {
        log.info("🔹 Checking if payment already exists for Order ID: {}", payment.getOrderId());
        Payment pendingPayment = paymentService.createPendingPayment(payment);
        return ResponseEntity.ok(pendingPayment);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getPaymentByOrderId(@PathVariable Long orderId) {
        Payment payment = paymentService.getPaymentByOrderId(orderId);
        if (payment == null) {
            return ResponseEntity.status(HttpStatus.SC_NOT_FOUND)
                    .body(Map.of("message", "Payment not found for Order ID: " + orderId));
        }
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/status/{orderId}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable Long orderId) {
        Payment payment = paymentService.getPaymentByOrderId(orderId);
        if (payment == null) {
            return ResponseEntity.status(HttpStatus.SC_NOT_FOUND)
                    .body(Map.of("message", "Payment not found for Order ID: " + orderId));
        }
        return ResponseEntity.ok(Map.of("paymentStatus", payment.getPaymentStatus()));
    }
}
