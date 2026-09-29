package com.auth_service.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.auth_service.model.User;

@Service
public class OtpService {

    private static final String OTP_PREFIX = "auth:otp:";
    private static final long OTP_EXPIRY_MINUTES = 5;

    private final StringRedisTemplate redisTemplate;
    private final AuthUserCacheService authUserCacheService;

    public OtpService(
            StringRedisTemplate redisTemplate,
            AuthUserCacheService authUserCacheService) {

        this.redisTemplate = redisTemplate;
        this.authUserCacheService = authUserCacheService;
    }

    public void sendOtp(String mobile) {

        String otp = String.valueOf(
                (int) (Math.random() * 900000) + 100000
        );

        String key = OTP_PREFIX + mobile.trim();

        redisTemplate.opsForValue().set(
                key,
                otp,
                Duration.ofMinutes(OTP_EXPIRY_MINUTES)
        );

        System.out.println(
                "📤 OTP sent to " + mobile + ": " + otp
        );
    }

    public User verifyOtp(
            String mobile,
            String otp) {

        String key = OTP_PREFIX + mobile.trim();

        String storedOtp =
                redisTemplate.opsForValue().get(key);

        if (storedOtp == null) {
            return null;
        }

        if (!storedOtp.equals(otp)) {
            return null;
        }

        // OTP is valid → delete immediately to prevent reuse
        redisTemplate.delete(key);

        // Reuse existing mobile-user Redis cache
        return authUserCacheService.findUserByMobile(
                mobile.trim()
        );
    }
}