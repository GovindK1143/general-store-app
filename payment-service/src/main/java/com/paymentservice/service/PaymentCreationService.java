package com.paymentservice.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.paymentservice.model.Payment;
import com.paymentservice.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentCreationService {

    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment createPayment(
            Long orderId,
            Long userId,
            Double amount,
            Boolean simulateFailure) {

        Payment payment = new Payment();

        payment.setOrderId(orderId);
        payment.setUserId(userId);
        payment.setAmount(amount);

        if (Boolean.TRUE.equals(simulateFailure)) {

            payment.setPaymentStatus("FAILED");
            payment.setTransactionId(null);
            payment.setPaymentDate(LocalDateTime.now());

        } else {

            payment.setPaymentStatus("SUCCESS");
            payment.setTransactionId(generateTransactionId());
            payment.setPaymentDate(LocalDateTime.now());
        }

        return paymentRepository.saveAndFlush(payment);
    }

    private String generateTransactionId() {

        return "TXN-" +
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 16)
                        .toUpperCase();
    }
}