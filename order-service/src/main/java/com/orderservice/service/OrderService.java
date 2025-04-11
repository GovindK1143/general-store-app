package com.orderservice.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.orderservice.client.PaymentServiceClient;
import com.orderservice.client.ProductServiceClient;
import com.orderservice.model.Order;
import com.orderservice.model.Payment;
import com.orderservice.repository.OrderRepository;
import com.orderservice.repository.PaymentRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private PaymentServiceClient paymentServiceClient;
    
    @Autowired
    private ProductServiceClient productServiceClient;

    private static final String PAYMENT_STATUS_TOPIC = "payment.status.topic";
    private static final String PAYMENT_SERVICE_CB = "paymentServiceCB";
    
    // ✅ Fetch All Orders
    public List<Order> getAllOrders() {
        log.info("📌 Fetching all orders...");
        return orderRepository.findAll();
    }

    // ✅ Fetch Orders by User ID
    public List<Order> getOrdersByUserId(Long userId) {
        log.info("📌 Fetching orders for User ID: {}", userId);
        return orderRepository.findByUserId(userId);
    }

    // ✅ Fetch Order Details with Payment Info
    public Map<String, Object> getOrderWithPayment(Long orderId) {
        log.info("📌 Fetching order details for Order ID: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found for ID: " + orderId));

        Optional<Payment> paymentOptional = paymentRepository.findByOrderId(orderId);

        Map<String, Object> response = new HashMap<>();
        response.put("order", order);
        response.put("payment", paymentOptional.orElse(null));

        log.info("✅ Retrieved order details: {}", response);
        return response;
    }

    // ✅ Place Order & Call Payment Service via Feign Client
    public ResponseEntity<Map<String, Object>> placeOrder(Order order) {
        order.setOrderDate(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        Map<String, Object> response = new HashMap<>();
        response.put("order", savedOrder);

        try {
            // 🔹 Call Payment Service via Feign Client
            Map<String, Object> paymentResponse = processPayment(savedOrder.getId());

            if (paymentResponse == null) {
                log.warn("⚠️ Payment Service did not return a valid response for Order ID: {}", savedOrder.getId());
                savePendingPayment(savedOrder.getId());
                response.put("paymentStatus", "PENDING");
            } else {
                response.put("paymentStatus", "PROCESSING");
            }
        } catch (Exception e) {
            log.error("⚠️ Payment processing failed for Order ID: {}", savedOrder.getId(), e);
            savePendingPayment(savedOrder.getId());
            response.put("paymentStatus", "PENDING");
        }

        return ResponseEntity.ok(response);
    }

    // ✅ Feign Client Call with Circuit Breaker
    @CircuitBreaker(name = PAYMENT_SERVICE_CB, fallbackMethod = "paymentServiceFallback")
    public Map<String, Object> processPayment(Long orderId) {
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("orderId", orderId);

        // ✅ Fetch order details
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        paymentRequest.put("amount", order.getTotalPrice());

        // 🔹 Call Payment Service via Feign Client
        Map<String, Object> paymentResponse = paymentServiceClient.processPayment(paymentRequest);

        if (paymentResponse == null) {
            log.warn("⚠️ Payment Service returned null for Order ID: {}", orderId);
            throw new RuntimeException("Payment Service did not respond properly");
        }

        // ✅ Prevent duplicate stock updates
        if ("SUCCESS".equals(paymentResponse.get("paymentStatus"))) {
            Payment existingPayment = paymentRepository.findByOrderId(orderId).orElse(null);
            if (existingPayment == null || !"SUCCESS".equals(existingPayment.getPaymentStatus())) {
                updateProductStock(order);
            }
        }

        return paymentResponse;
    }

    // ✅ Circuit Breaker Fallback Method
    public Map<String, Object> paymentServiceFallback(Long orderId, Exception ex) {
        log.warn("⚠️ Payment Service is down. Setting Payment Status to PENDING for Order ID: {}", orderId);
        savePendingPayment(orderId);

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", orderId);
        response.put("paymentStatus", "PENDING");
        response.put("message", "Payment Service is unavailable. Please retry later.");
        return response;
    }

    private void updateProductStock(Order order) {
        Map<String, Object> stockUpdateRequest = new HashMap<>();
        stockUpdateRequest.put("productId", order.getProductId());
        stockUpdateRequest.put("quantity", order.getQuantity());

        try {
            // 🔐 Fetch the JWT token from SecurityContext
            String jwtToken = extractJwtFromSecurityContext();

            // Pass the token to the Feign client
            productServiceClient.updateProductStock(stockUpdateRequest);
            log.info("✅ Stock updated for Product ID: {}", order.getProductId());
        } catch (Exception e) {
            log.error("⚠️ Failed to update stock for Product ID: {}", order.getProductId(), e);
        }
    }
    private String extractJwtFromSecurityContext() {
        org.springframework.security.core.context.SecurityContext context =
            org.springframework.security.core.context.SecurityContextHolder.getContext();

        if (context.getAuthentication() != null &&
            context.getAuthentication().getCredentials() instanceof String) {
            return context.getAuthentication().getCredentials().toString();
        }

        throw new RuntimeException("JWT token not found in security context");
    }


    // ✅ Save "PENDING" Payment to ORDER-SERVICE DB
    private void savePendingPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Payment pendingPayment = new Payment();
        pendingPayment.setOrderId(order.getId());
        pendingPayment.setAmount(order.getTotalPrice());
        pendingPayment.setPaymentStatus("PENDING");
        pendingPayment.setTransactionId(null);
        pendingPayment.setPaymentDate(LocalDateTime.now());

        paymentRepository.save(pendingPayment);
    }
}
