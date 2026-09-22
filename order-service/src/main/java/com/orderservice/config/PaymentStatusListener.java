package com.orderservice.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.orderservice.client.ProductServiceClient;
import com.orderservice.model.Order;
import com.orderservice.model.OrderItem;
import com.orderservice.model.PaymentStatusMessage;
import com.orderservice.repository.OrderRepository;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PaymentStatusListener {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductServiceClient productServiceClient;

    private static final int MAX_RETRIES = 5;

    private static final long RETRY_DELAY_MS = 1000;


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


        if (message.getOrderId() == null) {

            log.warn(
                    "Ignoring payment status message without orderId"
            );

            return;
        }


        Order order =
                findOrderWithRetry(
                        message.getOrderId()
                );


        if (order == null) {

            log.error(
                    "Order still not found after {} retries. " +
                            "orderId={}. Payment event could not be applied.",
                    MAX_RETRIES,
                    message.getOrderId()
            );

            return;
        }


        String paymentStatus =
                message.getPaymentStatus();


        // =========================================================
        // PAYMENT SUCCESS
        // =========================================================

        if ("SUCCESS".equalsIgnoreCase(
                paymentStatus)) {


            // -----------------------------------------------------
            // Idempotency
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
            // Update stock for ALL order items
            // -----------------------------------------------------

            boolean stockUpdated =
                    updateProductStock(order);


            if (!stockUpdated) {

                log.error(
                        "Payment succeeded but stock update failed. " +
                                "orderId={}",
                        order.getId()
                );


                order.setPaymentStatus(
                        "SUCCESS"
                );

                order.setOrderStatus(
                        "PENDING"
                );

                order.setStockUpdated(
                        false
                );

                orderRepository.save(order);

                return;
            }


            // -----------------------------------------------------
            // Stock update succeeded
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
            // PAYMENT PENDING
            // =========================================================

        } else {

            order.setPaymentStatus(
                    "PENDING"
            );

            order.setOrderStatus(
                    "PENDING"
            );
        }


        orderRepository.save(order);


        log.info(
                "Order payment status updated successfully. " +
                        "orderId={}, orderStatus={}, paymentStatus={}",
                order.getId(),
                order.getOrderStatus(),
                order.getPaymentStatus()
        );
    }


    // =========================================================
    // BATCH STOCK UPDATE
    // =========================================================

    private boolean updateProductStock(
            Order order) {

        List<Map<String, Object>> items =
                new ArrayList<>();


        for (OrderItem orderItem :
                order.getItems()) {

            Map<String, Object> item =
                    new HashMap<>();

            item.put(
                    "productId",
                    orderItem.getProductId()
            );

            item.put(
                    "quantity",
                    orderItem.getQuantity()
            );

            items.add(item);
        }


        if (items.isEmpty()) {

            log.error(
                    "Order contains no items. " +
                            "orderId={}",
                    order.getId()
            );

            return false;
        }


        Map<String, Object> request =
                new HashMap<>();

        request.put(
                "items",
                items
        );


        try {

            productServiceClient
                    .updateProductStockBatch(
                            request
                    );


            log.info(
                    "Product stock batch updated successfully. " +
                            "orderId={}, itemCount={}",
                    order.getId(),
                    items.size()
            );

            return true;


        } catch (Exception exception) {

            log.error(
                    "Failed to update product stock batch. " +
                            "orderId={}",
                    order.getId(),
                    exception
            );

            return false;
        }
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