package com.paymentservice.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.paymentservice.model.Payment;
import com.paymentservice.repository.PaymentRepository;

import jakarta.transaction.Transactional;

@Service
public class PaymentService {
    @Autowired
    private PaymentRepository paymentRepository;

    @Transactional // ✅ Ensures transaction commits before returning
    public Payment processPayment(Payment payment) {
        if (payment.getAmount() == 0) {
            throw new RuntimeException("Amount is missing for Order ID: " + payment.getOrderId());
        }

        payment.setPaymentStatus("SUCCESS");
        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setPaymentDate(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    public Payment createPendingPayment(Payment payment) {
        Payment existingPayment = paymentRepository.findByOrderId(payment.getOrderId());

        if (existingPayment != null) {
            return existingPayment; // Return existing pending payment
        }

        payment.setPaymentStatus("PENDING");
        payment.setTransactionId(null);
        payment.setPaymentDate(LocalDateTime.now());

        return paymentRepository.save(payment); // ✅ Save in PAYMENT-SERVICE DB
    }


    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }
}


