package com.auth_service.service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.auth_service.model.User;
import com.auth_service.repository.UserRepository;

@Service
public class OtpService {

    private final Map<String, String> otpStore = new ConcurrentHashMap<>();

    @Autowired
    private UserRepository userRepository;

    public void sendOtp(String mobile) {
        String otp = String.valueOf(new Random().nextInt(899999) + 100000);
        otpStore.put(mobile, otp);
        System.out.println("📤 OTP sent to " + mobile + ": " + otp);
        // TODO: Integrate with SMS provider
    }

    public User verifyOtp(String mobile, String otp) {
        if (!otp.equals(otpStore.getOrDefault(mobile, ""))) return null;
        otpStore.remove(mobile);
        return userRepository.findByMobile(mobile).orElse(null);
    }
}

