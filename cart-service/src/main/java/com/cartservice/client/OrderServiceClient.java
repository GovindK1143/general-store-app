package com.cartservice.client;

import com.cartservice.dto.CreateOrderRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "order-service")
public interface OrderServiceClient {

    @PostMapping("/orders/place")
    Map<String, Object> placeOrder(
            @RequestBody CreateOrderRequest request
    );
}