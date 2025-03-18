package com.auth_service.security;

import java.security.Key;
import java.util.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUt {
    private static final String SECRET_KEY = "mysecretmysecretmysecretmysecret"; // At least 32 chars
    private final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes()); // Correct key initialization

    private Date convertToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()); // No casting needed
    }

    public String generateToken(String email, String role) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expirationTime = now.plusHours(1); // Token valid for 1 hour

        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(convertToDate(now)) // Convert LocalDateTime to Date
                .setExpiration(convertToDate(expirationTime)) // Convert expiration time
                .signWith(key, SignatureAlgorithm.HS256) // Use correct key
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String extractRole(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
    }

    public boolean validateToken(String token, String email) {
        return extractEmail(token).equals(email) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        Date expiration = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();

        return expiration.before(new Date());
    }
}
