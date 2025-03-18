package com.orderservice.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orderservice.model.Order;
import com.orderservice.service.OrderService;

@RestController
@RequestMapping("/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;

    // ✅ Place Order API with Payment Processing
    @PostMapping("/place")
    public ResponseEntity<Map<String, Object>> placeOrder(@RequestBody Order order) {
        return orderService.placeOrder(order);
    }

    // ✅ Fetch All Orders
    @GetMapping("/all")
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    // ✅ Fetch Orders by User ID
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getOrdersByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    // ✅ Fetch Order Details with Payment Info
    @GetMapping("/{orderId}/details")
    public ResponseEntity<Map<String, Object>> getOrderWithPayment(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderWithPayment(orderId));
    }

    @PostMapping("/{orderId}/retry-payment")
    public ResponseEntity<Map<String, Object>> retryPayment(@PathVariable Long orderId) {
        return orderService.retryPendingPayment(orderId);
    }
}
