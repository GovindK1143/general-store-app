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
            String jwtToken = extractJwtFromSecurityContext(); // Use user's token
            requestTemplate.header("Authorization", "Bearer " + jwtToken);
        };
    }

    private String extractJwtFromSecurityContext() {
        org.springframework.security.core.context.SecurityContext context =
                org.springframework.security.core.context.SecurityContextHolder.getContext();

        if (context.getAuthentication() != null &&
            context.getAuthentication().getCredentials() instanceof String) {
            return context.getAuthentication().getCredentials().toString();
        }

        throw new RuntimeException("JWT token not found in security context");
    }

}

