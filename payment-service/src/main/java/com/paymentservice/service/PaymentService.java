package com.paymentservice.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

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
    private PaymentCreationService paymentCreationService;

    @Autowired
    private KafkaTemplate<String, PaymentStatusMessage> kafkaTemplate;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {

        /*
         * Check whether a payment already exists for this order.
         */
        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElse(null);

        /*
         * Existing successful payment.
         *
         * This is the normal idempotency path.
         */
        if (payment != null &&
                "SUCCESS".equalsIgnoreCase(payment.getPaymentStatus())) {

            publishPaymentStatus(payment);

            return PaymentResponse.fromPayment(payment);
        }

        /*
         * Existing FAILED/PENDING payment.
         *
         * Retry the existing payment.
         */
        if (payment != null) {

            payment.setAmount(request.getAmount());

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

            publishPaymentStatus(payment);

            return PaymentResponse.fromPayment(payment);
        }

        /*
         * No payment exists yet.
         *
         * Try to create the payment in a separate transaction.
         *
         * If another concurrent request creates the payment first,
         * the database UNIQUE constraint on order_id will reject
         * this insert.
         */
        try {

            payment = paymentCreationService.createPayment(
                    request.getOrderId(),
                    request.getUserId(),
                    request.getAmount(),
                    request.getSimulateFailure()
            );

        } catch (DataIntegrityViolationException exception) {

            /*
             * Another concurrent request created the payment first.
             *
             * Read the payment that was created by that request.
             */
            payment = paymentRepository
                    .findByOrderId(request.getOrderId())
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "Payment creation conflict for order ID: "
                                            + request.getOrderId(),
                                    exception
                            )
                    );
        }

        /*
         * Publish the payment record that exists in the database.
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