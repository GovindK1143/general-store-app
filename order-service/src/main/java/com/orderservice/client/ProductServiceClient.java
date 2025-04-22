package com.orderservice.client;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.orderservice.dto.ProductResponse;
import com.orderservice.security.FeignClientConfig;

@FeignClient(name = "PRODUCT-SERVICE", configuration = FeignClientConfig.class)
public interface ProductServiceClient {

    @PostMapping("/products/update-stock")
    void updateProductStock(@RequestBody Map<String, Object> stockUpdateRequest);
    
    @GetMapping("/products/id/{productId}")
    ProductResponse getProductById(@PathVariable("productId") Long productId);

}
