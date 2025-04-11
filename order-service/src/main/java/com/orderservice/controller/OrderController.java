package com.orderservice.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orderservice.model.Order;
import com.orderservice.service.OrderService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/orders")
@Slf4j
public class OrderController {
    @Autowired
    private OrderService orderService;

    @PostMapping("/place")
    public ResponseEntity<Map<String, Object>> placeOrder(@RequestBody Order order) {
        log.info("📌 Received request to place order: {}", order);
        ResponseEntity<Map<String, Object>> response = orderService.placeOrder(order);
        log.info("✅ Order placed successfully. Response: {}", response.getBody());
        return response;
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Order>> getAllOrders() {
        log.info("📌 Fetching all orders...");
        List<Order> orders = orderService.getAllOrders();
        log.info("✅ Retrieved {} orders.", orders.size());
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<Order>> getOrdersByUserId(@PathVariable Long userId) {
        log.info("📌 Fetching orders for User ID: {}", userId);
        List<Order> userOrders = orderService.getOrdersByUserId(userId);
        log.info("✅ Retrieved {} orders for User ID: {}", userOrders.size(), userId);
        return ResponseEntity.ok(userOrders);
    }

    @GetMapping("/{orderId}/details")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getOrderWithPayment(@PathVariable Long orderId) {
        log.info("📌 Fetching order details for Order ID: {}", orderId);
        ResponseEntity<Map<String, Object>> response = ResponseEntity.ok(orderService.getOrderWithPayment(orderId));
        log.info("✅ Order details retrieved: {}", response.getBody());
        return response;
    }
}
