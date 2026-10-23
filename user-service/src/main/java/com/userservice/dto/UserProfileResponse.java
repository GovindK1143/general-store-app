package com.userservice.dto;

import com.userservice.model.User;

public record UserProfileResponse(
        Long userId,
        String name,
        String email,
        String mobile,
        String address,
        String role
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getMobile(),
                user.getAddress(),
                user.getRole()
        );
    }
}