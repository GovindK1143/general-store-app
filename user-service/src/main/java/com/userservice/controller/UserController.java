package com.userservice.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

import com.userservice.dto.UpdateMyProfileRequest;
import com.userservice.dto.UserProfileResponse;
import com.userservice.model.User;
import com.userservice.service.UserService;
import com.userservice.dto.AdminCustomerResponse;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            Authentication authentication) {

        User user = userService.getUserByEmail(
                authentication.getName()
        );

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                UserProfileResponse.from(user)
        );
    }

    // =========================================================
    // ADMIN: GET ALL CUSTOMERS
    // =========================================================

    @GetMapping("/admin/customers")
    public ResponseEntity<List<AdminCustomerResponse>> getAllCustomersForAdmin() {
        return ResponseEntity.ok(
                userService.getAllCustomersForAdmin()
        );
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateMyProfileRequest request) {

        User updatedUser = userService.updateMyProfile(
                authentication.getName(),
                request
        );

        if (updatedUser == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                UserProfileResponse.from(updatedUser)
        );
    }

    // Preserve the existing endpoint for current integrations.
    @GetMapping("/{email}")
    public User getUser(@PathVariable String email) {
        return userService.getUserByEmail(email);
    }
}