package com.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-service", url = "http://AUTH-SERVICE/auth")
public interface AuthServiceClient {
    
    @GetMapping("/secret-key")
    String getSecretKey(@RequestHeader("Authorization") String authorizationHeader);
}



