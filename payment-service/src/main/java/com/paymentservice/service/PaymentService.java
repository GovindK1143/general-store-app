package com.paymentservice.service;

import com.paymentservice.dto.PaymentRequest;
import com.paymentservice.dto.PaymentResponse;
import com.paymentservice.model.Payment;
import com.paymentservice.model.PaymentStatusMessage;
import com.paymentservice.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {

    private static final String PAYMENT_STATUS_TOPIC =
            "payment.status.topic";

    private static final String PAYMENT_CACHE =
            "paymentsByOrder";

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentCreationService paymentCreationService;

    @Autowired
    private PaymentCacheService paymentCacheService;

    @Autowired
    private KafkaTemplate<String, PaymentStatusMessage> kafkaTemplate;


    // =========================================================
    // PROCESS PAYMENT
    // =========================================================

    @Transactional
    public PaymentResponse processPayment(
            PaymentRequest request,
            Long authenticatedUserId,
            String role) {

        validateAuthenticatedUser(
                request.getUserId(),
                authenticatedUserId,
                role
        );

        Payment payment =
                paymentRepository
                        .findByOrderId(request.getOrderId())
                        .orElse(null);


        /*
         * If a payment already exists, make sure a CUSTOMER
         * is allowed to access that payment.
         */
        if (payment != null) {

            validatePaymentOwnership(
                    payment,
                    authenticatedUserId,
                    role
            );
        }


        /*
         * Existing successful payment.
         *
         * This is the normal idempotency path.
         */
        if (payment != null &&
                "SUCCESS".equalsIgnoreCase(
                        payment.getPaymentStatus()
                )) {

            paymentCacheService.evictPayment(
                    request.getOrderId()
            );

            publishPaymentStatus(payment);

            return PaymentResponse.fromPayment(
                    payment
            );
        }


        /*
         * Existing FAILED/PENDING payment.
         *
         * Retry the existing payment.
         */
        if (payment != null) {

            payment.setAmount(
                    request.getAmount()
            );

            if (Boolean.TRUE.equals(
                    request.getSimulateFailure()
            )) {

                payment.setPaymentStatus(
                        "FAILED"
                );

                payment.setTransactionId(
                        null
                );

                payment.setPaymentDate(
                        LocalDateTime.now()
                );

            } else {

                payment.setPaymentStatus(
                        "SUCCESS"
                );

                payment.setTransactionId(
                        generateTransactionId()
                );

                payment.setPaymentDate(
                        LocalDateTime.now()
                );
            }

            payment =
                    paymentRepository.save(
                            payment
                    );

            paymentCacheService.evictPayment(
                    payment.getOrderId()
            );

            publishPaymentStatus(payment);

            return PaymentResponse.fromPayment(
                    payment
            );
        }


        /*
         * No payment exists yet.
         *
         * Try to create the payment in a separate transaction.
         *
         * The database UNIQUE constraint on order_id protects
         * against concurrent payment creation.
         */
        try {

            payment =
                    paymentCreationService.createPayment(
                            request.getOrderId(),
                            request.getUserId(),
                            request.getAmount(),
                            request.getSimulateFailure()
                    );

        } catch (DataIntegrityViolationException exception) {

            /*
             * Another concurrent request created the payment first.
             */
            payment =
                    paymentRepository
                            .findByOrderId(
                                    request.getOrderId()
                            )
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "Payment creation conflict for order ID: "
                                                    + request.getOrderId(),
                                            exception
                                    )
                            );

            /*
             * Make sure the authenticated CUSTOMER can access
             * the payment that won the race.
             */
            validatePaymentOwnership(
                    payment,
                    authenticatedUserId,
                    role
            );
        }


        /*
         * A new payment now exists in the database.
         */
        paymentCacheService.evictPayment(
                payment.getOrderId()
        );

        publishPaymentStatus(payment);

        return PaymentResponse.fromPayment(
                payment
        );
    }


    // =========================================================
    // GET PAYMENT BY ORDER ID
    // =========================================================

    @Cacheable(
            value = PAYMENT_CACHE,
            key = "'order:' + #orderId + ':user:' + #authenticatedUserId + ':role:' + #role",
            unless = "#result == null"
    )
    public PaymentResponse getPaymentByOrderId(
            Long orderId,
            Long authenticatedUserId,
            String role) {

        validateOrderId(orderId);

        Payment payment =
                paymentRepository
                        .findByOrderId(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Payment not found for order ID: "
                                                + orderId
                                )
                        );

        validatePaymentOwnership(
                payment,
                authenticatedUserId,
                role
        );

        return PaymentResponse.fromPayment(
                payment
        );
    }


    // =========================================================
    // GET PAYMENT STATUS
    // =========================================================

    @Cacheable(
            value = PAYMENT_CACHE,
            key = "'order:' + #orderId + ':user:' + #authenticatedUserId + ':role:' + #role",
            unless = "#result == null"
    )
    public PaymentResponse getPaymentStatus(
            Long orderId,
            Long authenticatedUserId,
            String role) {

        validateOrderId(orderId);

        Payment payment =
                paymentRepository
                        .findByOrderId(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Payment not found for order ID: "
                                                + orderId
                                )
                        );

        validatePaymentOwnership(
                payment,
                authenticatedUserId,
                role
        );

        return PaymentResponse.fromPayment(
                payment
        );
    }


    // =========================================================
    // AUTHENTICATED USER VALIDATION
    // =========================================================

    private void validateAuthenticatedUser(
            Long requestUserId,
            Long authenticatedUserId,
            String role) {

        if (authenticatedUserId == null) {

            throw new AccessDeniedException(
                    "Authenticated user ID is missing"
            );
        }

        /*
         * ADMIN can process payments for any user.
         */
        if ("ADMIN".equalsIgnoreCase(role)) {
            return;
        }

        /*
         * CUSTOMER can process only their own payment.
         */
        if ("CUSTOMER".equalsIgnoreCase(role)) {

            if (!authenticatedUserId.equals(requestUserId)) {

                throw new AccessDeniedException(
                        "You are not authorized to process payment for another user"
                );
            }

            return;
        }

        throw new AccessDeniedException(
                "Unauthorized role"
        );
    }


    // =========================================================
    // PAYMENT OWNERSHIP VALIDATION
    // =========================================================

    private void validatePaymentOwnership(
            Payment payment,
            Long authenticatedUserId,
            String role) {

        /*
         * ADMIN can view/process any payment.
         */
        if ("ADMIN".equalsIgnoreCase(role)) {
            return;
        }

        /*
         * CUSTOMER can access only their own payment.
         */
        if ("CUSTOMER".equalsIgnoreCase(role)
                && authenticatedUserId != null
                && authenticatedUserId.equals(
                payment.getUserId()
        )) {

            return;
        }

        throw new AccessDeniedException(
                "You are not authorized to access this payment"
        );
    }


    // =========================================================
    // ORDER ID VALIDATION
    // =========================================================

    private void validateOrderId(Long orderId) {

        if (orderId == null || orderId <= 0) {

            throw new IllegalArgumentException(
                    "Order ID must be greater than zero"
            );
        }
    }


    // =========================================================
    // PUBLISH PAYMENT STATUS
    // =========================================================

    private void publishPaymentStatus(
            Payment payment) {

        PaymentStatusMessage message =
                new PaymentStatusMessage();

        message.setOrderId(
                payment.getOrderId()
        );

        message.setUserId(
                payment.getUserId()
        );

        message.setAmount(
                payment.getAmount()
        );

        message.setPaymentStatus(
                payment.getPaymentStatus()
        );

        message.setTransactionId(
                payment.getTransactionId()
        );

        message.setPaymentDate(
                payment.getPaymentDate()
        );

        kafkaTemplate.send(
                PAYMENT_STATUS_TOPIC,
                String.valueOf(
                        payment.getOrderId()
                ),
                message
        );
    }


    // =========================================================
    // GENERATE TRANSACTION ID
    // =========================================================

    private String generateTransactionId() {

        return "TXN-" +
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 16)
                        .toUpperCase();
    }
}