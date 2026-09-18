package com.auth_service.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String tokenType;
    private Long userId;
    private String name;
    private String email;
    private String mobile;
    private String role;
}