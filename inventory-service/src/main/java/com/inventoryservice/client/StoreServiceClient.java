
package com.inventoryservice.client;

import com.inventoryservice.dto.StoreServiceResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "STORE-SERVICE",
        contextId = "storeServiceClient"
)
public interface StoreServiceClient {

    @GetMapping("/stores/{id}")
    StoreServiceResponse getStoreById(
            @PathVariable("id") Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION)
            String authorization
    );
}
