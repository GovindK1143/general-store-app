package com.orderservice.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import com.orderservice.dto.CustomerAddressResponse;
import com.orderservice.security.FeignClientConfig;

@FeignClient(
        name = "USER-SERVICE",
        configuration = FeignClientConfig.class
)
public interface UserServiceClient {

    @GetMapping("/users/me/addresses")
    List<CustomerAddressResponse> getMyAddresses();
}