package com.userservice.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class UserCacheService {

    private static final String USER_CACHE = "usersByEmail";

    @CacheEvict(
            value = USER_CACHE,
            key = "#email"
    )
    public void evictUser(String email) {

        // Cache eviction is handled by Spring Cache.
    }
}