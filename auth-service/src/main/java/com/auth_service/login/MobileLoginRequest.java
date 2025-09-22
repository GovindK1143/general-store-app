package com.auth_service.login;

import lombok.Data;

@Data
public class MobileLoginRequest {
    private String mobile;
    private String password;
}