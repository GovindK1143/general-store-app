package com.orderservice.client;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.orderservice.security.FeignClientConfig;

@FeignClient(
        name = "PAYMENT-SERVICE",
        configuration = FeignClientConfig.class
)
public interface PaymentServiceClient {

    @PostMapping("/payments/process")
    Map<String, Object> processPayment(
            @RequestBody Map<String, Object> paymentRequest
    );

    @GetMapping("/payments/order/{orderId}")
    Map<String, Object> getPaymentByOrderId(
            @PathVariable("orderId") Long orderId
    );
}