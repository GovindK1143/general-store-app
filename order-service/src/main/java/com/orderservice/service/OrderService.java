package com.orderservice.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.orderservice.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderservice.client.PaymentServiceClient;
import com.orderservice.client.ProductServiceClient;
import com.orderservice.exception.InvalidOrderException;
import com.orderservice.exception.OrderNotFoundException;
import com.orderservice.model.Order;
import com.orderservice.model.OrderItem;
import com.orderservice.repository.OrderRepository;
import com.orderservice.dto.OrderItemResponse;
import com.orderservice.dto.OrderResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderService {

    private static final String PAYMENT_SERVICE_CB =
            "paymentServiceCB";

    private static final String ORDER_PENDING =
            "PENDING";

    private static final String ORDER_CONFIRMED =
            "CONFIRMED";

    private static final String ORDER_CANCELLED =
            "CANCELLED";

    private static final String PAYMENT_PENDING =
            "PENDING";

    private static final String PAYMENT_SUCCESS =
            "SUCCESS";

    private static final String PAYMENT_FAILED =
            "FAILED";

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentServiceClient paymentServiceClient;

    @Autowired
    private ProductServiceClient productServiceClient;


    // =========================================================
    // GET ALL ORDERS
    // =========================================================
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {

        log.info("Fetching all orders");

        return orderRepository.findAll()
                .stream()
                .map(OrderResponse::fromOrder)
                .toList();
    }


    // =========================================================
    // GET USER ORDERS
    // =========================================================
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(Long userId) {

        if (userId == null) {
            throw new InvalidOrderException(
                    "User ID is required"
            );
        }

        log.info(
                "Fetching orders for User ID: {}",
                userId
        );

        return orderRepository
                .findByUserIdOrderByOrderDateDesc(userId)
                .stream()
                .map(OrderResponse::fromOrder)
                .toList();
    }


    // =========================================================
    // PLACE MULTI-PRODUCT ORDER
    // =========================================================

    @Transactional
    public OrderResponse placeOrder(
            CreateOrderRequest request,
            Long userId) {

        if (userId == null) {

            throw new InvalidOrderException(
                    "Authenticated user ID is missing"
            );
        }

        if (request == null ||
                request.getItems() == null ||
                request.getItems().isEmpty()) {

            throw new InvalidOrderException(
                    "Order must contain at least one item"
            );
        }

        log.info(
                "Creating order. userId={}, itemCount={}",
                userId,
                request.getItems().size()
        );

        // ---------------------------------------------------------
        // Validate duplicate products
        // ---------------------------------------------------------

        Set<Long> productIds = new HashSet<>();

        for (OrderItemRequest itemRequest :
                request.getItems()) {

            if (itemRequest == null) {

                throw new InvalidOrderException(
                        "Order item is required"
                );
            }

            if (itemRequest.getProductId() == null) {

                throw new InvalidOrderException(
                        "Product ID is required"
                );
            }

            if (!productIds.add(
                    itemRequest.getProductId())) {

                throw new InvalidOrderException(
                        "Duplicate product ID in order: "
                                + itemRequest.getProductId()
                );
            }
        }


        // ---------------------------------------------------------
        // Create Order
        // ---------------------------------------------------------

        Order order = new Order();

        order.setUserId(userId);
        order.setOrderDate(LocalDateTime.now());

        order.setOrderStatus(ORDER_PENDING);
        order.setPaymentStatus(PAYMENT_PENDING);
        order.setStockUpdated(false);

        order.setItems(new ArrayList<>());


        double totalAmount = 0.0;


        // ---------------------------------------------------------
        // Validate every product and create OrderItems
        // ---------------------------------------------------------

        for (OrderItemRequest itemRequest :
                request.getItems()) {

            if (itemRequest == null ||
                    itemRequest.getProductId() == null) {

                throw new InvalidOrderException(
                        "Product ID is required"
                );
            }

            if (itemRequest.getQuantity() == null ||
                    itemRequest.getQuantity() <= 0) {

                throw new InvalidOrderException(
                        "Quantity must be greater than zero"
                );
            }


            log.info(
                    "Validating product. productId={}, quantity={}",
                    itemRequest.getProductId(),
                    itemRequest.getQuantity()
            );


            // -----------------------------------------------------
            // Get product from Product Service
            // -----------------------------------------------------

            ProductResponse product =
                    productServiceClient.getProductById(
                            itemRequest.getProductId()
                    );


            if (product == null) {

                throw new InvalidOrderException(
                        "Product not found with ID: "
                                + itemRequest.getProductId()
                );
            }


            // -----------------------------------------------------
            // Validate product
            // -----------------------------------------------------

            if (!Boolean.TRUE.equals(
                    product.getActive())) {

                throw new InvalidOrderException(
                        "Product is currently unavailable: "
                                + product.getName()
                );
            }

            if (product.getSellingPrice() == null) {

                throw new InvalidOrderException(
                        "Product selling price is not available: "
                                + product.getName()
                );
            }

            if (product.getStock() == null) {

                throw new InvalidOrderException(
                        "Product stock information is not available: "
                                + product.getName()
                );
            }

            if (product.getStock() <
                    itemRequest.getQuantity()) {

                throw new InvalidOrderException(
                        "Insufficient stock for product: "
                                + product.getName()
                                + ". Available stock: "
                                + product.getStock()
                );
            }


            // -----------------------------------------------------
            // Calculate item subtotal
            // -----------------------------------------------------

            double subtotal =
                    product.getSellingPrice()
                            * itemRequest.getQuantity();


            // -----------------------------------------------------
            // Create OrderItem
            // -----------------------------------------------------

            OrderItem orderItem = new OrderItem();

            orderItem.setProductId(
                    product.getId()
            );

            orderItem.setProductName(
                    product.getName()
            );

            orderItem.setQuantity(
                    itemRequest.getQuantity()
            );

            orderItem.setUnitPrice(
                    product.getSellingPrice()
            );

            orderItem.setSubtotal(
                    subtotal
            );


            order.addItem(orderItem);

            totalAmount += subtotal;
        }


        // ---------------------------------------------------------
        // Set total amount
        // ---------------------------------------------------------

        order.setTotalAmount(totalAmount);


        // ---------------------------------------------------------
        // Save Order + OrderItems
        // ---------------------------------------------------------

        Order savedOrder =
                orderRepository.save(order);


        log.info(
                "Order created successfully. " +
                        "orderId={}, userId={}, totalAmount={}",
                savedOrder.getId(),
                userId,
                totalAmount
        );


        // ---------------------------------------------------------
        // Process payment
        // ---------------------------------------------------------

        Map<String, Object> paymentResponse;

        try {

            paymentResponse =
                    processPayment(savedOrder);

        } catch (Exception exception) {

            log.warn(
                    "Payment processing failed for orderId={}. " +
                            "Keeping payment PENDING. Reason: {}",
                    savedOrder.getId(),
                    exception.getMessage()
            );

            paymentResponse = new HashMap<>();

            paymentResponse.put(
                    "paymentStatus",
                    PAYMENT_PENDING
            );
        }


        // ---------------------------------------------------------
        // Check payment status
        // ---------------------------------------------------------

        String paymentStatus =
                getStringValue(
                        paymentResponse,
                        "paymentStatus"
                );


        if (paymentStatus == null) {

            paymentStatus = PAYMENT_PENDING;
        }


        // ---------------------------------------------------------
        // PAYMENT SUCCESS
        // ---------------------------------------------------------

        if (PAYMENT_SUCCESS.equalsIgnoreCase(
                paymentStatus)) {

            /*
             * Payment succeeded.
             *
             * Stock will be updated by Kafka listener.
             */

            savedOrder.setPaymentStatus(
                    PAYMENT_SUCCESS
            );

            savedOrder.setOrderStatus(
                    ORDER_PENDING
            );

            savedOrder.setStockUpdated(false);

            orderRepository.save(savedOrder);


            log.info(
                    "Payment successful. Waiting for Kafka " +
                            "event to update stock. orderId={}",
                    savedOrder.getId()
            );


            // ---------------------------------------------------------
            // PAYMENT FAILED
            // ---------------------------------------------------------

        } else if (PAYMENT_FAILED.equalsIgnoreCase(
                paymentStatus)) {

            savedOrder.setPaymentStatus(
                    PAYMENT_FAILED
            );

            savedOrder.setOrderStatus(
                    ORDER_CANCELLED
            );

            savedOrder.setStockUpdated(false);

            orderRepository.save(savedOrder);


            log.warn(
                    "Payment failed. Order cancelled. " +
                            "orderId={}",
                    savedOrder.getId()
            );


            // ---------------------------------------------------------
            // PAYMENT PENDING
            // ---------------------------------------------------------

        } else {

            savedOrder.setPaymentStatus(
                    PAYMENT_PENDING
            );

            savedOrder.setOrderStatus(
                    ORDER_PENDING
            );

            savedOrder.setStockUpdated(false);

            orderRepository.save(savedOrder);


            log.info(
                    "Payment pending. orderId={}",
                    savedOrder.getId()
            );
        }


        return OrderResponse.fromOrder(
                savedOrder
        );
    }


    // =========================================================
    // PAYMENT SERVICE
    // =========================================================

    @CircuitBreaker(
            name = PAYMENT_SERVICE_CB,
            fallbackMethod = "paymentServiceFallback"
    )
    public Map<String, Object> processPayment(
            Order order) {

        Map<String, Object> paymentRequest =
                new HashMap<>();

        paymentRequest.put(
                "orderId",
                order.getId()
        );

        paymentRequest.put(
                "userId",
                order.getUserId()
        );

        paymentRequest.put(
                "amount",
                order.getTotalAmount()
        );


        log.info(
                "Calling Payment Service. " +
                        "orderId={}, amount={}",
                order.getId(),
                order.getTotalAmount()
        );


        Map<String, Object> paymentResponse =
                paymentServiceClient.processPayment(
                        paymentRequest
                );


        if (paymentResponse == null) {

            throw new RuntimeException(
                    "Payment Service returned empty response"
            );
        }

        return paymentResponse;
    }


    // =========================================================
    // PAYMENT SERVICE FALLBACK
    // =========================================================

    public Map<String, Object> paymentServiceFallback(
            Order order,
            Exception exception) {

        log.warn(
                "Payment Service unavailable for orderId={}. " +
                        "Payment will remain PENDING. Reason: {}",
                order.getId(),
                exception.getMessage()
        );


        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "orderId",
                order.getId()
        );

        response.put(
                "paymentStatus",
                PAYMENT_PENDING
        );

        response.put(
                "message",
                "Payment Service is unavailable. " +
                        "Please retry payment later."
        );

        return response;
    }


    // =========================================================
    // GET ORDER STATUS
    // =========================================================

    public OrderStatusResponse getOrderStatus(
            Long orderId,
            Long userId,
            boolean admin) {

        Order order;


        if (admin) {

            order =
                    orderRepository.findById(orderId)
                            .orElseThrow(() ->
                                    new OrderNotFoundException(
                                            "Order not found with ID: "
                                                    + orderId
                                    )
                            );

        } else {

            order =
                    orderRepository
                            .findByIdAndUserId(
                                    orderId,
                                    userId
                            )
                            .orElseThrow(() ->
                                    new OrderNotFoundException(
                                            "Order not found with ID: "
                                                    + orderId
                                    )
                            );
        }


        String transactionId = null;

        LocalDateTime paymentDate = null;

        String paymentStatus =
                order.getPaymentStatus();


        // ---------------------------------------------------------
        // Get latest payment information
        // ---------------------------------------------------------

        try {

            Map<String, Object> payment =
                    paymentServiceClient
                            .getPaymentByOrderId(
                                    orderId
                            );


            if (payment != null &&
                    !payment.isEmpty()) {

                String servicePaymentStatus =
                        getStringValue(
                                payment,
                                "paymentStatus"
                        );


                if (servicePaymentStatus != null) {

                    paymentStatus =
                            servicePaymentStatus;
                }


                transactionId =
                        getStringValue(
                                payment,
                                "transactionId"
                        );


                paymentDate =
                        getLocalDateTimeValue(
                                payment,
                                "paymentDate"
                        );
            }

        } catch (Exception exception) {

            log.warn(
                    "Unable to retrieve payment information " +
                            "for orderId={}: {}",
                    orderId,
                    exception.getMessage()
            );
        }


        return OrderStatusResponse.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .items(
                        order.getItems()
                                .stream()
                                .map(OrderItemResponse::fromOrderItem)
                                .toList()
                )
                .totalAmount(order.getTotalAmount())
                .orderDate(order.getOrderDate())
                .orderStatus(order.getOrderStatus())
                .paymentStatus(paymentStatus)
                .transactionId(transactionId)
                .paymentDate(paymentDate)
                .build();
    }


    // =========================================================
    // MAP VALUE HELPERS
    // =========================================================

    private String getStringValue(
            Map<String, Object> data,
            String key) {

        if (data == null) {
            return null;
        }

        Object value = data.get(key);

        return value != null
                ? value.toString()
                : null;
    }


    private LocalDateTime getLocalDateTimeValue(
            Map<String, Object> data,
            String key) {

        if (data == null) {
            return null;
        }

        Object value = data.get(key);

        if (value == null) {
            return null;
        }

        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }

        try {

            return LocalDateTime.parse(
                    value.toString()
            );

        } catch (Exception exception) {

            return null;
        }
    }
}