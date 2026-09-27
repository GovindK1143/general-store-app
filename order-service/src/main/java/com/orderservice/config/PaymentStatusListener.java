package com.orderservice.config;

import java.util.Optional;

import com.orderservice.model.Order;
import com.orderservice.model.PaymentStatusMessage;
import com.orderservice.repository.OrderRepository;
import com.orderservice.service.ProductStockService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PaymentStatusListener {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductStockService productStockService;

    private static final int MAX_RETRIES = 5;

    private static final long RETRY_DELAY_MS = 1000;


    // =========================================================
    // KAFKA PAYMENT STATUS LISTENER
    // =========================================================

    @KafkaListener(
            topics = "payment.status.topic",
            groupId = "order-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handlePaymentStatus(
            PaymentStatusMessage message) {

        log.info(
                "Received payment status. orderId={}, status={}, transactionId={}",
                message.getOrderId(),
                message.getPaymentStatus(),
                message.getTransactionId()
        );


        // =========================================================
        // VALIDATE ORDER ID
        // =========================================================

        if (message.getOrderId() == null) {

            log.warn(
                    "Ignoring payment status message without orderId"
            );

            return;
        }


        // =========================================================
        // FIND ORDER
        // =========================================================

        Order order =
                findOrderWithRetry(
                        message.getOrderId()
                );


        if (order == null) {

            log.error(
                    "Order still not found after {} retries. " +
                            "orderId={}. Kafka will retry the payment event.",
                    MAX_RETRIES,
                    message.getOrderId()
            );

            /*
             * IMPORTANT:
             *
             * Do not simply return here.
             *
             * Throwing the exception tells Spring Kafka that
             * processing failed. The configured Kafka
             * DefaultErrorHandler will then retry the message.
             */
            throw new IllegalStateException(
                    "Order not found after retries. orderId="
                            + message.getOrderId()
            );
        }


        String paymentStatus =
                message.getPaymentStatus();


        // =========================================================
        // PAYMENT SUCCESS
        // =========================================================

        if ("SUCCESS".equalsIgnoreCase(
                paymentStatus)) {


            // -----------------------------------------------------
            // IDEMPOTENCY CHECK
            // -----------------------------------------------------

            if (Boolean.TRUE.equals(
                    order.getStockUpdated())) {

                log.info(
                        "Stock already updated. " +
                                "Skipping duplicate stock update. orderId={}",
                        order.getId()
                );

                return;
            }


            // -----------------------------------------------------
            // UPDATE STOCK FOR ALL ORDER ITEMS
            //
            // ProductStockService contains Resilience4j @Retry.
            //
            // If all Resilience4j attempts fail, the exception
            // is deliberately propagated to Kafka.
            // -----------------------------------------------------

            try {

                productStockService.updateProductStock(
                        order
                );

            } catch (Exception exception) {

                log.error(
                        "Stock update failed after Resilience4j retries. " +
                                "orderId={}. Kafka will retry this message.",
                        order.getId(),
                        exception
                );

                /*
                 * IMPORTANT:
                 *
                 * Do NOT swallow this exception.
                 *
                 * Spring Kafka must know that processing failed
                 * so that DefaultErrorHandler can perform the
                 * configured Kafka-level retries.
                 */
                throw exception;
            }


            // -----------------------------------------------------
            // STOCK UPDATE SUCCESSFUL
            // -----------------------------------------------------

            order.setPaymentStatus(
                    "SUCCESS"
            );

            order.setOrderStatus(
                    "CONFIRMED"
            );

            order.setStockUpdated(
                    true
            );


            // =========================================================
            // PAYMENT FAILED
            // =========================================================

        } else if ("FAILED".equalsIgnoreCase(
                paymentStatus)) {

            order.setPaymentStatus(
                    "FAILED"
            );

            order.setOrderStatus(
                    "CANCELLED"
            );


            // =========================================================
            // PAYMENT PENDING / UNKNOWN STATUS
            // =========================================================

        } else {

            order.setPaymentStatus(
                    "PENDING"
            );

            order.setOrderStatus(
                    "PENDING"
            );
        }


        // =========================================================
        // SAVE ORDER
        // =========================================================

        orderRepository.save(
                order
        );


        log.info(
                "Order payment status updated successfully. " +
                        "orderId={}, orderStatus={}, paymentStatus={}",
                order.getId(),
                order.getOrderStatus(),
                order.getPaymentStatus()
        );
    }


    // =========================================================
    // FIND ORDER WITH RETRY
    // =========================================================

    private Order findOrderWithRetry(
            Long orderId) {

        for (
                int attempt = 1;
                attempt <= MAX_RETRIES;
                attempt++
        ) {

            Optional<Order> orderOptional =
                    orderRepository.findById(
                            orderId
                    );


            if (orderOptional.isPresent()) {

                if (attempt > 1) {

                    log.info(
                            "Order found after retry. " +
                                    "orderId={}, attempt={}",
                            orderId,
                            attempt
                    );
                }

                return orderOptional.get();
            }


            if (attempt < MAX_RETRIES) {

                log.warn(
                        "Order not found yet. " +
                                "orderId={}, attempt={}/{}. Retrying...",
                        orderId,
                        attempt,
                        MAX_RETRIES
                );


                try {

                    Thread.sleep(
                            RETRY_DELAY_MS
                    );

                } catch (InterruptedException exception) {

                    Thread.currentThread().interrupt();

                    log.error(
                            "Retry interrupted for orderId={}",
                            orderId
                    );

                    return null;
                }
            }
        }


        return null;
    }
}