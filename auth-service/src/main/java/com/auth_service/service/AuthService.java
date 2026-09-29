package com.auth_service.service;

import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
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
    private final AuthUserCacheService authUserCacheService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder, AuthUserCacheService authUserCacheService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authUserCacheService = authUserCacheService;
    }

    @Transactional
    public User registerUser(RegisterRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase();

        String mobile = request.getMobile()
                .trim();

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
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        // Never allow customer registration
        // to select the role.
        user.setRole(Role.CUSTOMER);

        return userRepository.save(user);
    }

    @Transactional
    public User registerAdmin(RegisterRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase();

        String mobile = request.getMobile()
                .trim();

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
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        user.setRole(Role.ADMIN);

        return userRepository.save(user);
    }

    public User authenticate(String username, String password) {

        String loginId = username.trim();

        User user;

        if (loginId.contains("@")) {

            user = authUserCacheService.findUserByEmail(loginId);

        } else {

            user = authUserCacheService.findUserByMobile(loginId);
        }

        if (user == null) {
            return null;
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return null;
        }

        return user;
    }
}