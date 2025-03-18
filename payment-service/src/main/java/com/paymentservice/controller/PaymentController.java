package com.paymentservice.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.apache.hc.core5.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paymentservice.model.Payment;
import com.paymentservice.service.PaymentService;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody Payment payment) {
        Payment savedPayment = paymentService.processPayment(payment);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", savedPayment.getId());
        response.put("orderId", savedPayment.getOrderId());
        response.put("paymentStatus", savedPayment.getPaymentStatus());
        response.put("transactionId", savedPayment.getTransactionId());
        response.put("paymentDate", savedPayment.getPaymentDate());
        response.put("amount",savedPayment.getAmount());

        return ResponseEntity.ok(response);
    }

    // ✅ New API: Create "PENDING" Payment Entry **Only if it Doesn't Exist**
    @PostMapping("/createPending")
    public ResponseEntity<Payment> createPendingPayment(@RequestBody Payment payment) {
        Payment existingPayment = paymentService.getPaymentByOrderId(payment.getOrderId());

        if (existingPayment != null) {
            System.out.println("ℹ️ Payment already exists for Order ID: " + payment.getOrderId());
            return ResponseEntity.ok(existingPayment);
        }

        payment.setPaymentStatus("PENDING");
        payment.setTransactionId("N/A");
        payment.setPaymentDate(LocalDateTime.now());
        return ResponseEntity.ok(paymentService.createPendingPayment(payment));
    }

    @GetMapping("/order/{orderId}")
    public Payment getPaymentByOrderId(@PathVariable Long orderId) {
        return paymentService.getPaymentByOrderId(orderId);
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

