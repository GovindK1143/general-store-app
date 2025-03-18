package com.auth_service.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auth_service.login.LoginRequest;
import com.auth_service.model.User;
import com.auth_service.repository.UserRepository;
import com.auth_service.security.JwtUt;
import com.auth_service.service.AuthService;

@RestController
@RequestMapping("/auth")
public class AuthController {
	
    @Autowired
    private AuthService authService;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUt JwtUt;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public User registerUser(@RequestBody User user) {
        return authService.registerUser(user);
    }

    @PostMapping("/register-admin")
    public User registerAdmin(@RequestBody User user) {
        return authService.registerAdmin(user);
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail());
        
        if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = JwtUt.generateToken(user.getEmail(), user.getRole().name());

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        return response;
    }
}
