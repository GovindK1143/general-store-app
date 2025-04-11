package com.orderservice.security;

import feign.RequestInterceptor;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Configuration
public class FeignClientConfig {

    @Value("${jwt.secret}")
    private String secretKeyString;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            String token = generateServiceToken();
            requestTemplate.header("Authorization", "Bearer " + token);
        };
    }

    private String generateServiceToken() {
        Key secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setSubject("order-service")
                .claim("role", "SERVICE")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 600_000))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }
}

