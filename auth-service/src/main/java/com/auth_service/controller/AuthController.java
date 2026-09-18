package com.auth_service.controller;

import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.auth_service.login.LoginRequest;
import com.auth_service.login.RegisterRequest;
import com.auth_service.model.User;
import com.auth_service.response.LoginResponse;
import com.auth_service.response.UserResponse;
import com.auth_service.security.JwtUtil;
import com.auth_service.service.AuthService;
import com.auth_service.service.OtpService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;

    public AuthController(
            AuthService authService,
            JwtUtil jwtUtil,
            OtpService otpService) {

        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.otpService = otpService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(
            @Valid @RequestBody RegisterRequest request) {

        User user = authService.registerUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(UserResponse.from(user));
    }

    @PostMapping("/register-admin")
    public ResponseEntity<UserResponse> registerAdmin(
            @Valid @RequestBody RegisterRequest request) {

        User user = authService.registerAdmin(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(UserResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request) {

        User user = authService.authenticate(
                request.getUsername(),
                request.getPassword()
        );

        if (user == null) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message",
                            "Invalid username or password"
                    ));
        }

        return ResponseEntity.ok(
                createLoginResponse(user)
        );
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(
            @RequestBody Map<String, String> body) {

        String mobile = body.get("mobile");

        if (mobile == null || mobile.isBlank()) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "success", false,
                            "message",
                            "Mobile number is required"
                    ));
        }

        otpService.sendOtp(mobile);

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message",
                        "OTP sent to mobile"
                )
        );
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(
            @RequestBody Map<String, String> body) {

        User user = otpService.verifyOtp(
                body.get("mobile"),
                body.get("otp")
        );

        if (user == null) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message",
                            "Invalid or expired OTP"
                    ));
        }

        return ResponseEntity.ok(
                createLoginResponse(user)
        );
    }

    private LoginResponse createLoginResponse(User user) {

        String token = jwtUtil.generateToken(user);

        return new LoginResponse(
                token,
                "Bearer",
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getMobile(),
                user.getRole().name()
        );
    }
}