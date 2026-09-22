package com.orderservice.security;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class ServiceJwtUtil {

    @Value("${jwt.secret}")
    private String secretKeyString;

    private static final long EXPIRATION_TIME =
            60 * 60 * 1000L; // 1 hour

    private Key getSigningKey() {

        return Keys.hmacShaKeyFor(
                secretKeyString.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateServiceToken() {

        return Jwts.builder()
                .setSubject("order-service")
                .claim("role", "ADMIN")
                .setIssuedAt(new Date())
                .setExpiration(
                        new Date(
                                System.currentTimeMillis()
                                        + EXPIRATION_TIME
                        )
                )
                .signWith(
                        getSigningKey(),
                        SignatureAlgorithm.HS256
                )
                .compact();
    }
}