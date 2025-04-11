package com.orderservice.client;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "PRODUCT-SERVICE", configuration = com.orderservice.security.FeignClientConfig.class)
public interface ProductServiceClient {

    @PostMapping("/products/update-stock")
    void updateProductStock(@RequestBody Map<String, Object> stockUpdateRequest);
}
