package com.orderservice.client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "PAYMENT-SERVICE")
public interface PaymentServiceClient {

    @PostMapping("/payments/process")
    Map<String, Object> processPayment(@RequestBody Map<String, Object> paymentRequest);

    @GetMapping("/payments/order/{orderId}")
    Map<String, Object> getPaymentByOrderId(@PathVariable Long orderId);
}
