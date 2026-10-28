package com.userservice.dto;

import com.userservice.model.User;

public record AdminCustomerResponse(
        Long userId,
        String name,
        String email,
        String role,
        String mobile,
        String address
) {
    public static AdminCustomerResponse from(User user) {
        return new AdminCustomerResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getMobile(),
                user.getAddress()
        );
    }
}