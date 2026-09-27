package com.orderservice.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.orderservice.client.ProductServiceClient;
import com.orderservice.model.Order;
import com.orderservice.model.OrderItem;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductStockService {

    private final ProductServiceClient productServiceClient;

    /**
     * Updates stock for all products in an order.
     *
     * Retry is applied through Resilience4j.
     */
    @Retry(name = "productStockRetry")
    public void updateProductStock(Order order) {

        if (order == null) {

            throw new IllegalArgumentException(
                    "Order is required"
            );
        }

        if (order.getId() == null) {

            throw new IllegalArgumentException(
                    "Order ID is required for stock update"
            );
        }

        if (order.getItems() == null ||
                order.getItems().isEmpty()) {

            throw new IllegalStateException(
                    "Order contains no items. orderId="
                            + order.getId()
            );
        }

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

        Map<String, Object> request =
                new HashMap<>();

        // -----------------------------------------------------
        // IMPORTANT:
        // Send orderId so Product Service can perform
        // idempotency checking.
        // -----------------------------------------------------

        request.put(
                "orderId",
                order.getId()
        );

        request.put(
                "items",
                items
        );

        log.info(
                "Updating product stock. " +
                        "orderId={}, itemCount={}",
                order.getId(),
                items.size()
        );

        productServiceClient.updateProductStockBatch(
                request
        );

        log.info(
                "Product stock batch updated successfully. " +
                        "orderId={}, itemCount={}",
                order.getId(),
                items.size()
        );
    }
}