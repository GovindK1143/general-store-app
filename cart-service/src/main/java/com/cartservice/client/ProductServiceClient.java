package com.cartservice.client;

import com.cartservice.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @GetMapping("/products/id/{productId}")
    ProductResponse getProductById(
            @PathVariable("productId") Long productId
    );
}