package com.userservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.userservice.model.User;
import com.userservice.repository.UserRepository;

@Service
public class UserService {

    private static final String USER_CACHE = "usersByEmail";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCacheService userCacheService;

    public User registerUser(User user) {

        User savedUser = userRepository.save(user);

        /*
         * If the same email was previously cached,
         * remove the stale value.
         */
        userCacheService.evictUser(user.getEmail());

        return savedUser;
    }

    @Cacheable(
            value = USER_CACHE,
            key = "#email",
            unless = "#result == null"
    )
    public User getUserByEmail(String email) {

        return userRepository.findByEmail(email);
    }
}