package com.orderservice.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.orderservice.model.Order;
import com.orderservice.model.Payment;
import com.orderservice.repository.OrderRepository;
import com.orderservice.repository.PaymentRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.transaction.Transactional;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RestTemplate restTemplate;

    private static final String PAYMENT_SERVICE_CB = "paymentServiceCB";
    
    
    @Transactional
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void retryPendingPayments() {
        List<Payment> pendingPayments = paymentRepository.findByPaymentStatus("PENDING");

        for (Payment payment : pendingPayments) {
            System.out.println("🔄 Retrying payment for Order ID: " + payment.getOrderId());

            String paymentStatus = getPaymentStatusFromPaymentService(payment.getOrderId());

            if ("SUCCESS".equals(paymentStatus)) {
                System.out.println("✅ Payment already SUCCESS for Order ID: " + payment.getOrderId() + ". Updating Order DB.");

                // 🔹 Update Order DB with latest success status
                payment.setPaymentStatus("SUCCESS");
                payment.setTransactionId(getTransactionIdFromPaymentService(payment.getOrderId())); // ✅ Fetch latest transaction ID
                paymentRepository.save(payment);

                continue; // ✅ Stop retrying for this order
            }

            try {
                processPayment(payment.getOrderId());
            } catch (Exception e) {
                System.out.println("⚠️ Payment retry failed for Order ID: " + payment.getOrderId());
            }
        }
    }

    
    private String getTransactionIdFromPaymentService(Long orderId) {
    	try {
            String url = "http://PAYMENT-SERVICE/payments/order/" + orderId;
            ResponseEntity<Payment> response = restTemplate.getForEntity(url, Payment.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody().getTransactionId(); // ✅ Fetch latest transaction ID
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error fetching transaction ID: " + e.getMessage());
        }
        return "UNKNOWN"; // Default if error occurs
    }


    // ✅ Fetch payment status from PAYMENT-SERVICE
    private String getPaymentStatusFromPaymentService(Long orderId) {
        try {
            String url = "http://PAYMENT-SERVICE/payments/order/" + orderId;
            ResponseEntity<Payment> response = restTemplate.getForEntity(url, Payment.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody().getPaymentStatus();
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error fetching payment status: " + e.getMessage());
        }
        return "PENDING"; // Default to PENDING if an error occurs
    }



    // ✅ Place Order & Call Payment Service
    public ResponseEntity<Map<String, Object>> placeOrder(Order order) {
        order.setOrderDate(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        Map<String, Object> response = new HashMap<>();
        response.put("order", savedOrder);

        try {
            // 🔹 Call Payment Service to process payment
            Map<String, Object> paymentResponse = processPayment(savedOrder.getId());

            // ✅ Wait before first check to allow transaction to commit
            Thread.sleep(2000); // 🔴 Increased delay to ensure PAYMENT-SERVICE transaction is completed

            // ✅ Fetch payment status with retry
            String paymentStatus = fetchPaymentStatusWithRetry(savedOrder.getId());

            if ("SUCCESS".equalsIgnoreCase(paymentStatus)) {
                savePaymentRecord(savedOrder.getId(), paymentResponse, "SUCCESS"); // ✅ Save SUCCESS directly
                response.put("paymentStatus", "SUCCESS");
            } else {
                savePendingPayment(savedOrder.getId()); // ✅ Save as PENDING if still not success
                response.put("paymentStatus", "PENDING");
            }

        } catch (Exception e) {
            System.out.println("⚠️ Payment failed for Order " + savedOrder.getId());
            savePendingPayment(savedOrder.getId());
            response.put("paymentStatus", "PENDING");
        }

        return ResponseEntity.ok(response);
    }

    private String fetchPaymentStatusWithRetry(Long orderId) throws InterruptedException {
        int retryCount = 0;
        String paymentStatus = "PENDING";

        while (retryCount < 5) {
            paymentStatus = getPaymentStatusFromPaymentService(orderId);

            if ("SUCCESS".equalsIgnoreCase(paymentStatus)) {
                break; // ✅ Exit loop if payment is successful
            }

            Thread.sleep(800); // 🔴 Increased delay to 800ms before retrying
            retryCount++;
        }

        return paymentStatus;
    }


    // ✅ Call Payment Service with Circuit Breaker
    @CircuitBreaker(name = PAYMENT_SERVICE_CB, fallbackMethod = "paymentServiceFallback")
    public Map<String, Object> processPayment(Long orderId) {
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("orderId", orderId);

        // ✅ Fetch order details
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        paymentRequest.put("amount", order.getTotalPrice());

        String paymentServiceUrl = "http://PAYMENT-SERVICE/payments/process";
        Map<String, Object> paymentResponse = restTemplate.postForObject(paymentServiceUrl, paymentRequest, Map.class);

        // ✅ Prevent duplicate stock updates
        if ("SUCCESS".equals(paymentResponse.get("paymentStatus"))) {
            Payment existingPayment = paymentRepository.findByOrderId(orderId).orElse(null);
            if (existingPayment == null || !"SUCCESS".equals(existingPayment.getPaymentStatus())) {
                updateProductStock(order);  // ✅ Only update stock if first time
            }
            savePaymentRecord(orderId, paymentResponse, "SUCCESS");
        }

        return paymentResponse;
    }
    
    private void updateProductStock(Order order) {
        Map<String, Object> stockUpdateRequest = new HashMap<>();
        stockUpdateRequest.put("productId", order.getProductId());
        stockUpdateRequest.put("quantity", order.getQuantity());

        String productServiceUrl = "http://PRODUCT-SERVICE/products/update-stock";
        restTemplate.postForObject(productServiceUrl, stockUpdateRequest, Void.class);

        System.out.println("✅ Stock updated for Product ID: " + order.getProductId());
    }

    // ✅ Fallback Method: If Payment Service Fails
    public void paymentServiceFallback(Long orderId, Exception ex) {
        System.out.println("⚠️ Payment Service is down. Setting Payment Status to PENDING.");
        savePendingPayment(orderId);
    }

    // ✅ Save "PENDING" Payment to ORDER-SERVICE DB
    private void savePendingPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Payment pendingPayment = new Payment();
        pendingPayment.setOrderId(order.getId());
        pendingPayment.setAmount(order.getTotalPrice());  // ✅ Set correct amount
        pendingPayment.setPaymentStatus("PENDING");
        pendingPayment.setTransactionId(null);
        pendingPayment.setPaymentDate(LocalDateTime.now());

        paymentRepository.save(pendingPayment);  // ✅ Fixed: Now paymentRepository is correctly initialized
    }

    // ✅ Fetch All Orders
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    // ✅ Fetch Orders by User ID
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    // ✅ Fetch Order with Payment Details
    public Map<String, Object> getOrderWithPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Call Payment Service to get payment details
        String paymentServiceUrl = "http://PAYMENT-SERVICE/payments/order/" + orderId;
        Map<String, Object> paymentDetails = restTemplate.getForObject(paymentServiceUrl, Map.class);

        // ✅ Combine Order & Payment Details
        Map<String, Object> response = new HashMap<>();
        response.put("order", order);
        response.put("payment", paymentDetails);

        return response;
    }
    
    private void savePaymentRecord(Long orderId, Map<String, Object> paymentResponse, String status) {
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);

        if (payment == null) {
            payment = new Payment();
            payment.setOrderId(orderId);
        }
        
        payment.setAmount(Double.parseDouble(paymentResponse.get("amount").toString()));
        payment.setPaymentStatus(status);
        payment.setPaymentDate(LocalDateTime.now());

        if ("SUCCESS".equals(status)) {
            payment.setTransactionId(paymentResponse.get("transactionId").toString()); // ✅ Update Transaction ID
        }

        paymentRepository.save(payment);
        System.out.println("✅ Payment status updated: Order " + orderId + " → " + status);
    }




    public ResponseEntity<Map<String, Object>> retryPendingPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Optional<Payment> pendingPayment = paymentRepository.findByOrderId(orderId);
        
        // ✅ Fix: Checking null instead of isEmpty()
        if (pendingPayment == null || !"PENDING".equals(pendingPayment.get().getPaymentStatus())) {
            throw new RuntimeException("No pending payment found for Order ID: " + orderId);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("order", order);

        try {
            // ✅ Calling PAYMENT-SERVICE for retry
            processPayment(orderId);
            response.put("paymentStatus", "SUCCESS");
        } catch (Exception e) {
            System.out.println("⚠️ Payment retry failed for Order ID: " + orderId);
            response.put("paymentStatus", "PENDING");
        }

        return ResponseEntity.ok(response);
    }

}