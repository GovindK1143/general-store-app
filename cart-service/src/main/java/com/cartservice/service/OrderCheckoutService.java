package com.cartservice.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.cartservice.client.OrderServiceClient;
import com.cartservice.dto.CreateOrderRequest;
import com.cartservice.exception.CartException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderCheckoutService {

    private static final String ORDER_SERVICE_CB = "orderServiceCB";

    private final OrderServiceClient orderServiceClient;

    @CircuitBreaker(
            name = ORDER_SERVICE_CB,
            fallbackMethod = "orderServiceFallback"
    )
    public Map<String, Object> placeOrder(CreateOrderRequest request) {

        log.info("Calling Order Service from OrderCheckoutService");

        return orderServiceClient.placeOrder(request);
    }

    public Map<String, Object> orderServiceFallback(
            CreateOrderRequest request,
            Exception exception) {

        log.error(
                "Order Service unavailable during checkout. "
                        + "Circuit Breaker fallback triggered. Reason: {}",
                exception.getMessage()
        );

        throw new CartException(
                "Order Service is currently unavailable. "
                        + "Please try again later."
        );
    }
}