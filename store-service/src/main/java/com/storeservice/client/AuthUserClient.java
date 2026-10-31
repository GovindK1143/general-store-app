package com.storeservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import com.storeservice.dto.AuthUserResponse;

@FeignClient(name = "AUTH-SERVICE")
public interface AuthUserClient {

    @GetMapping("/auth/admin/users/{userId}")
    AuthUserResponse getUserByIdForAdmin(
            @PathVariable("userId") Long userId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    );
}