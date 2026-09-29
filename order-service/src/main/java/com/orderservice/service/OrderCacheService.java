package com.orderservice.service;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class OrderCacheService {

    private static final String USER_ORDERS_CACHE = "userOrders";

    private final CacheManager cacheManager;

    public OrderCacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictUserOrders(Long userId) {

        if (userId == null) {
            return;
        }

        Cache cache = cacheManager.getCache(USER_ORDERS_CACHE);

        if (cache != null) {
            cache.evict("user:" + userId);
        }
    }
}