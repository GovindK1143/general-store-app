package com.auth_service.response;

import com.auth_service.model.User;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponse {

    private Long userId;
    private String name;
    private String email;
    private String mobile;
    private String role;

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getMobile(),
                user.getRole().name()
        );
    }
}