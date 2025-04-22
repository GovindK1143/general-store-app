package com.paymentservice.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.paymentservice.model.Payment;
import com.paymentservice.model.PaymentStatusMessage;
import com.paymentservice.repository.PaymentRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private static final String PAYMENT_STATUS_TOPIC = "payment.status.topic";

    public Payment processPayment(Payment payment) {
        log.info("📌 Processing payment for Order ID: {}, Amount: {}", payment.getOrderId(), payment.getAmount());

        // Check for duplicate successful payments
        Optional<Payment> existingPayment = paymentRepository.findByOrderId(payment.getOrderId());
        if (existingPayment.isPresent() && "SUCCESS".equals(existingPayment.get().getPaymentStatus())) {
            log.warn("⚠️ Payment already processed for Order ID: {}", payment.getOrderId());
            throw new RuntimeException("Duplicate payment attempt detected");
        }

        // Set payment details
        payment.setPaymentStatus("SUCCESS");
        payment.setTransactionId("TXN-" + System.currentTimeMillis());
        payment.setPaymentDate(LocalDateTime.now());

        // Save to payment_db
        Payment savedPayment = paymentRepository.save(payment);

        // 🔥 Publish to Kafka (with correct amount)
        PaymentStatusMessage message = new PaymentStatusMessage(
            savedPayment.getOrderId(),
            savedPayment.getPaymentStatus(),
            savedPayment.getTransactionId(),
            savedPayment.getAmount() // ✅ Make sure amount is populated
        );

        kafkaTemplate.send(PAYMENT_STATUS_TOPIC, message);
        log.info("📤 Published payment status to Kafka: {}", message);

        return savedPayment;
    }

    public Payment createPendingPayment(Payment payment) {
        return paymentRepository.findByOrderId(payment.getOrderId()).orElseGet(() -> {
            payment.setPaymentStatus("PENDING");
            payment.setPaymentDate(LocalDateTime.now());
            return paymentRepository.save(payment);
        });
    }

    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId).orElse(null);
    }
}
