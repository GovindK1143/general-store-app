package com.userservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import com.userservice.dto.AdminCustomerResponse;
import com.userservice.dto.UpdateMyProfileRequest;
import com.userservice.model.User;
import com.userservice.repository.UserRepository;

@Service
public class UserService {

    private static final String USER_CACHE = "usersByEmail";

    @Autowired
    private UserRepository userRepository;

    @Cacheable(
            value = USER_CACHE,
            key = "#email",
            unless = "#result == null"
    )
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = USER_CACHE, key = "#email"),
            @CacheEvict(value = "authUsersByEmail", allEntries = true),
            @CacheEvict(value = "authUsersByMobile", allEntries = true)
    })
    public User updateMyProfile(
            String email,
            UpdateMyProfileRequest request) {

        User user = userRepository.findByEmail(email);

        if (user == null) {
            return null;
        }

        user.setName(request.name().trim());

        return userRepository.save(user);
    }

    // =========================================================
    // ADMIN: GET ALL CUSTOMERS
    // =========================================================

    public List<AdminCustomerResponse> getAllCustomersForAdmin() {
        return userRepository.findAll()
                .stream()
                .filter(user ->
                        "CUSTOMER".equalsIgnoreCase(user.getRole())
                )
                .map(AdminCustomerResponse::from)
                .toList();
    }
}