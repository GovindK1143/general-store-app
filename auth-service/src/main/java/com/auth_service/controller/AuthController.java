package com.auth_service.controller;

import java.util.HashMap;
import java.util.Map;

import org.apache.hc.core5.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auth_service.login.LoginRequest;
import com.auth_service.login.MobileLoginRequest;
import com.auth_service.model.User;
import com.auth_service.repository.UserRepository;
import com.auth_service.security.JwtUtil;
import com.auth_service.service.AuthService;
import com.auth_service.service.OtpService;

@RestController
@RequestMapping("/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private OtpService otpService;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    public User registerUser(@RequestBody User user) {
        return authService.registerUser(user);
    }

    @PostMapping("/register-admin")
    public User registerAdmin(@RequestBody User user) {
        return authService.registerAdmin(user);
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        User user = authService.authenticate(loginRequest.getEmail(), loginRequest.getPassword());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.SC_UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("userId", user.getId());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login-email")
    public ResponseEntity<?> loginWithEmail(@RequestBody LoginRequest request) {
        User user = authService.authenticate(request.getEmail(), request.getPassword());
        return generateLoginResponse(user);
    }

    @PostMapping("/login-mobile")
    public ResponseEntity<?> loginWithMobile(@RequestBody MobileLoginRequest request) {
        User user = authService.authenticateByMobile(request.getMobile(), request.getPassword());
        return generateLoginResponse(user);
    }
    
    private ResponseEntity<?> generateLoginResponse(User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.SC_UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return ResponseEntity.ok(Map.of(
            "token", token,
            "userId", user.getId(),
            "name", user.getName(),
            "email", user.getEmail(),
            "role", user.getRole()
        ));
    }
    
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> body) {
        otpService.sendOtp(body.get("mobile"));
        return ResponseEntity.ok("OTP sent to mobile");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        User user = otpService.verifyOtp(body.get("mobile"), body.get("otp"));
        return generateLoginResponse(user);
    }

    
}


