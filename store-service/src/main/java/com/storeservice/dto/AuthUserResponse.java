package com.storeservice.dto;

public record AuthUserResponse(
        Long userId,
        String name,
        String email,
        String mobile,
        String role
) {
}