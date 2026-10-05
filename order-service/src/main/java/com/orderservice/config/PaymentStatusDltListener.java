package com.orderservice.config;

import com.orderservice.model.Order;
import com.orderservice.model.PaymentStatusMessage;
import com.orderservice.repository.OrderRepository;
import com.orderservice.service.OrderCacheService;
import com.orderservice.service.ProductStockService;

import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PaymentStatusDltListener {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductStockService productStockService;

    @Autowired
    private OrderCacheService orderCacheService;

    @KafkaListener(
            topics = "payment.status.topic-dlt",
            groupId = "order-dlt-recovery-group",
            containerFactory = "dltKafkaListenerContainerFactory"
    )
    @Transactional
    public void handleDltPaymentStatus(PaymentStatusMessage message) {

        log.info(
                "Received payment status from DLT. orderId={}, status={}, transactionId={}",
                message.getOrderId(),
                message.getPaymentStatus(),
                message.getTransactionId()
        );

        if (message.getOrderId() == null) {
            log.warn("Ignoring DLT payment message without orderId");
            return;
        }

        Order order = orderRepository
                .findById(message.getOrderId())
                .orElse(null);

        if (order == null) {
            log.error(
                    "Order not found for DLT message. orderId={}",
                    message.getOrderId()
            );

            throw new IllegalStateException(
                    "Order not found for DLT message. orderId="
                            + message.getOrderId()
            );
        }

        if ("SUCCESS".equalsIgnoreCase(message.getPaymentStatus())) {

            if (Boolean.TRUE.equals(order.getStockUpdated())) {

                log.info(
                        "Stock already updated. Skipping DLT recovery. orderId={}",
                        order.getId()
                );

                return;
            }

            log.info(
                    "Recovering stock update from DLT. orderId={}",
                    order.getId()
            );

            productStockService.updateProductStock(order);

            order.setPaymentStatus("SUCCESS");
            order.setOrderStatus("CONFIRMED");
            order.setStockUpdated(true);

            orderRepository.save(order);

            // Evict cached order history after order update
            orderCacheService.evictUserOrders(
                    order.getUserId()
            );

            log.info(
                    "DLT recovery successful. orderId={}, orderStatus={}, paymentStatus={}",
                    order.getId(),
                    order.getOrderStatus(),
                    order.getPaymentStatus()
            );

        } else if ("FAILED".equalsIgnoreCase(message.getPaymentStatus())) {

            order.setPaymentStatus("FAILED");
            order.setOrderStatus("CANCELLED");

            orderRepository.save(order);

            // Evict cached order history after order update
            orderCacheService.evictUserOrders(
                    order.getUserId()
            );

            log.info(
                    "DLT payment failure applied to order. orderId={}",
                    order.getId()
            );

        } else {

            log.warn(
                    "Ignoring unsupported DLT payment status. orderId={}, status={}",
                    order.getId(),
                    message.getPaymentStatus()
            );
        }
    }
}