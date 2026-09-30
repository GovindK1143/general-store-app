package com.paymentservice.service;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class PaymentCacheService {

    private static final String PAYMENT_CACHE =
            "paymentsByOrder";

    private final CacheManager cacheManager;

    public PaymentCacheService(
            CacheManager cacheManager) {

        this.cacheManager = cacheManager;
    }

    public void evictPayment(Long orderId) {

        if (orderId == null) {
            return;
        }

        Cache cache =
                cacheManager.getCache(
                        PAYMENT_CACHE
                );

        if (cache != null) {

            cache.evict(
                    "order:" + orderId
            );
        }
    }
}