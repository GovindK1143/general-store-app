package com.paymentservice.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymentservice.dto.PaymentRequest;
import com.paymentservice.dto.PaymentResponse;
import com.paymentservice.model.Payment;
import com.paymentservice.model.PaymentStatusMessage;
import com.paymentservice.repository.PaymentRepository;

@Service
public class PaymentService {

    private static final String PAYMENT_STATUS_TOPIC = "payment.status.topic";

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private KafkaTemplate<String, PaymentStatusMessage> kafkaTemplate;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {

        /*
         * Check whether a payment already exists for this order.
         * This makes the payment operation idempotent.
         */
        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElse(null);

        /*
         * If payment was already successful, don't create another payment.
         */
        if (payment != null && "SUCCESS".equalsIgnoreCase(payment.getPaymentStatus())) {

            publishPaymentStatus(payment);

            return PaymentResponse.fromPayment(payment);
        }

        /*
         * Create a new payment or retry an existing pending/failed payment.
         */
        if (payment == null) {

            payment = new Payment();

            payment.setOrderId(request.getOrderId());
            payment.setUserId(request.getUserId());

        }

        payment.setAmount(request.getAmount());

        /*
         * For now this is a mock/simulated payment.
         *
         * Later we can replace this section with:
         * Razorpay / Stripe / PayU / payment gateway integration.
         */
        if (Boolean.TRUE.equals(request.getSimulateFailure())) {

            payment.setPaymentStatus("FAILED");

            payment.setTransactionId(null);

            payment.setPaymentDate(LocalDateTime.now());

        } else {

            payment.setPaymentStatus("SUCCESS");

            payment.setTransactionId(generateTransactionId());

            payment.setPaymentDate(LocalDateTime.now());
        }

        payment = paymentRepository.save(payment);

        /*
         * Notify Order Service through Kafka.
         */
        publishPaymentStatus(payment);

        return PaymentResponse.fromPayment(payment);
    }

    public PaymentResponse getPaymentByOrderId(Long orderId) {

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Payment not found for order ID: " + orderId));

        return PaymentResponse.fromPayment(payment);
    }

    public PaymentResponse getPaymentStatus(Long orderId) {

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Payment not found for order ID: " + orderId));

        return PaymentResponse.fromPayment(payment);
    }

    private void publishPaymentStatus(Payment payment) {

        PaymentStatusMessage message = new PaymentStatusMessage();

        message.setOrderId(payment.getOrderId());
        message.setUserId(payment.getUserId());
        message.setAmount(payment.getAmount());
        message.setPaymentStatus(payment.getPaymentStatus());
        message.setTransactionId(payment.getTransactionId());
        message.setPaymentDate(payment.getPaymentDate());

        kafkaTemplate.send(
                PAYMENT_STATUS_TOPIC,
                String.valueOf(payment.getOrderId()),
                message
        );
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