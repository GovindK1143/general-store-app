
package com.inventoryservice.security;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final Set<String> SUPPORTED_ROLES = Set.of(
            "ADMIN",
            "CUSTOMER",
            "STORE_MANAGER",
            "STORE_EMPLOYEE"
    );

    private final SecretKey signingKey;

    public JwtService(
            @Value("${jwt.secret}") String secret) {

        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT secret must not be empty"
            );
        }

        this.signingKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    private Claims extractAllClaims(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Long extractUserId(String token) {

        Object userId = extractAllClaims(token).get("userId");

        if (userId instanceof Number number) {
            return number.longValue();
        }

        if (userId instanceof String value) {
            return Long.valueOf(value);
        }

        return null;
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public boolean isTokenValid(String token) {

        try {
            Claims claims = extractAllClaims(token);

            String email = claims.getSubject();
            Long userId = extractUserId(token);
            String role = claims.get("role", String.class);

            return email != null
                    && !email.isBlank()
                    && userId != null
                    && userId > 0
                    && role != null
                    && SUPPORTED_ROLES.contains(role);

        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }
}
