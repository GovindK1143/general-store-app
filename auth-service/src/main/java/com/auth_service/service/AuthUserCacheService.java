package com.auth_service.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.auth_service.model.User;
import com.auth_service.repository.UserRepository;

@Service
public class AuthUserCacheService {

    private final UserRepository userRepository;

    public AuthUserCacheService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Cacheable(
            value = "authUsersByEmail",
            key = "#email.trim().toLowerCase()",
            unless = "#result == null"
    )
    public User findUserByEmail(String email) {

        return userRepository.findByEmailIgnoreCase(
                email.trim().toLowerCase()
        ).orElse(null);
    }

    @Cacheable(
            value = "authUsersByMobile",
            key = "#mobile.trim()",
            unless = "#result == null"
    )
    public User findUserByMobile(String mobile) {

        return userRepository.findByMobile(
                mobile.trim()
        ).orElse(null);
    }
}