
package com.inventoryservice.client;

import com.inventoryservice.dto.ProductServiceResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "PRODUCT-SERVICE",
        contextId = "productServiceClient"
)
public interface ProductServiceClient {

    @GetMapping("/products/id/{productId}")
    ProductServiceResponse getProductById(
            @PathVariable("productId") Long productId,
            @RequestHeader(HttpHeaders.AUTHORIZATION)
            String authorization
    );
}
