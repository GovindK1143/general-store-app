package com.auth_service.login;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Email or mobile number is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
