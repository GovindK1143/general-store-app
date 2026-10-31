
package com.storeservice.security;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    private static final Set<String> SUPPORTED_ROLES = Set.of(
            "ADMIN",
            "CUSTOMER",
            "STORE_MANAGER",
            "STORE_EMPLOYEE"
    );

    public Claims extractAllClaims(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(
                        Keys.hmacShaKeyFor(
                                secretKey.getBytes(
                                        StandardCharsets.UTF_8
                                )
                        )
                )
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public StoreUserPrincipal extractPrincipal(String token) {

        Claims claims = extractAllClaims(token);

        String email = claims.getSubject();
        Number userId = claims.get("userId", Number.class);
        String role = claims.get("role", String.class);

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT subject is missing"
            );
        }

        if (userId == null || userId.longValue() <= 0) {
            throw new IllegalArgumentException(
                    "Invalid user ID in JWT"
            );
        }

        if (role == null ||
                !SUPPORTED_ROLES.contains(role)) {

            throw new IllegalArgumentException(
                    "Unsupported role in JWT"
            );
        }

        return new StoreUserPrincipal(
                userId.longValue(),
                email,
                role
        );
    }
}
