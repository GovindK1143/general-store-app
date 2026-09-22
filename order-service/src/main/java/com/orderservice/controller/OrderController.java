package com.orderservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orderservice.dto.CreateOrderRequest;
import com.orderservice.dto.OrderResponse;
import com.orderservice.dto.OrderStatusResponse;
import com.orderservice.model.Order;
import com.orderservice.service.OrderService;
import com.orderservice.dto.OrderResponse;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/orders")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/place")
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestAttribute(name = "userId", required = false)
            Long userId) {

        log.info(
                "Received order request. itemCount={}, userId={}",
                request.getItems() != null
                        ? request.getItems().size()
                        : 0,
                userId
        );

        OrderResponse response =
                orderService.placeOrder(
                        request,
                        userId
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {

        log.info("Fetching all orders");

        return ResponseEntity.ok(
                orderService.getAllOrders()
        );
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByUserId(
            @PathVariable Long userId,
            @RequestAttribute(name = "userId", required = false)
            Long authenticatedUserId,
            Authentication authentication) {

        boolean admin = hasRole(
                authentication,
                "ROLE_ADMIN"
        );

        /*
         * ADMIN can request any user's orders.
         *
         * CUSTOMER can request only their own orders.
         */
        if (!admin &&
                (authenticatedUserId == null ||
                        !authenticatedUserId.equals(userId))) {

            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(
                orderService.getOrdersByUserId(userId)
        );
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<OrderStatusResponse> getOrderStatus(
            @PathVariable Long id,
            @RequestAttribute(name = "userId", required = false)
            Long userId,
            Authentication authentication) {

        boolean admin = hasRole(
                authentication,
                "ROLE_ADMIN"
        );

        OrderStatusResponse response =
                orderService.getOrderStatus(
                        id,
                        userId,
                        admin
                );

        return ResponseEntity.ok(response);
    }

    private boolean hasRole(
            Authentication authentication,
            String role) {

        if (authentication == null) {
            return false;
        }

        return authentication
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }
}