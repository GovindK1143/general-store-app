package com.orderservice.config;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.orderservice.model.Payment;
import com.orderservice.model.PaymentStatusMessage;
import com.orderservice.repository.PaymentRepository;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PaymentStatusListener {

    @Autowired
    private PaymentRepository paymentRepository;

    @KafkaListener(topics = "payment.status.topic", groupId = "order-group", containerFactory = "kafkaListenerContainerFactory")
    public void handlePaymentStatus(PaymentStatusMessage message) {
        log.info("📥 Received payment status message from Kafka: {}", message);

        Payment payment = new Payment();
        payment.setOrderId(message.getOrderId());
        payment.setPaymentStatus(message.getPaymentStatus());
        payment.setTransactionId(message.getTransactionId());
        payment.setAmount(message.getAmount()); 
        payment.setPaymentDate(LocalDateTime.now());

        paymentRepository.save(payment);

        log.info("💾 Payment record saved for Order ID: {}", message.getOrderId());
    }
}
