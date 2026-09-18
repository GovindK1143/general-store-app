package com.auth_service.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auth_service.exception.DuplicateUserException;
import com.auth_service.login.RegisterRequest;
import com.auth_service.model.Role;
import com.auth_service.model.User;
import com.auth_service.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(RegisterRequest request) {

        String email = request.getEmail().trim().toLowerCase();
        String mobile = request.getMobile().trim();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateUserException(
                    "Email is already registered"
            );
        }

        if (userRepository.existsByMobile(mobile)) {
            throw new DuplicateUserException(
                    "Mobile number is already registered"
            );
        }

        User user = new User();

        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setMobile(mobile);
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        // Never allow customer registration to select the role.
        user.setRole(Role.CUSTOMER);

        return userRepository.save(user);
    }

    @Transactional
    public User registerAdmin(RegisterRequest request) {

        String email = request.getEmail().trim().toLowerCase();
        String mobile = request.getMobile().trim();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateUserException(
                    "Email is already registered"
            );
        }

        if (userRepository.existsByMobile(mobile)) {
            throw new DuplicateUserException(
                    "Mobile number is already registered"
            );
        }

        User user = new User();

        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setMobile(mobile);
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        user.setRole(Role.ADMIN);

        return userRepository.save(user);
    }

    public User authenticate(String username, String password) {

        String loginId = username.trim();

        Optional<User> userOptional;

        if (loginId.contains("@")) {
            userOptional =
                    userRepository.findByEmailIgnoreCase(loginId);
        } else {
            userOptional =
                    userRepository.findByMobile(loginId);
        }

        if (userOptional.isEmpty()) {
            return null;
        }

        User user = userOptional.get();

        if (!passwordEncoder.matches(
                password,
                user.getPassword())) {

            return null;
        }

        return user;
    }
}