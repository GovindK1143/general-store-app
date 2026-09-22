package com.paymentservice.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secretKeyString;

    private Key secretKey;

    @PostConstruct
    public void initialize() {
        secretKey = Keys.hmacShaKeyFor(
                secretKeyString.getBytes(StandardCharsets.UTF_8)
        );

        log.info("JWT secret initialized successfully for Payment Service");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();

        if (token.isEmpty()) {
            sendUnauthorizedResponse(response, "JWT token is missing");
            return;
        }

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String email = claims.getSubject();
            String role = claims.get("role", String.class);
            Long userId = getUserId(claims);

            if (email == null || email.isBlank()) {
                sendUnauthorizedResponse(response, "JWT subject is missing");
                return;
            }

            if (role == null || role.isBlank()) {
                sendUnauthorizedResponse(response, "JWT role is missing");
                return;
            }

            String authority = "ROLE_" + role.toUpperCase();

            UserDetails userDetails = User
                    .withUsername(email)
                    .password("")
                    .authorities(new SimpleGrantedAuthority(authority))
                    .build();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            token,
                            userDetails.getAuthorities()
                    );

            request.setAttribute("userId", userId);
            request.setAttribute("email", email);
            request.setAttribute("role", role.toUpperCase());

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (Exception exception) {

            log.warn(
                    "JWT validation failed for {} {}: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    exception.getMessage()
            );

            SecurityContextHolder.clearContext();

            sendUnauthorizedResponse(
                    response,
                    "Invalid or expired JWT token"
            );
        }
    }

    private Long getUserId(Claims claims) {

        Object userIdClaim = claims.get("userId");

        if (userIdClaim == null) {
            return null;
        }

        if (userIdClaim instanceof Number number) {
            return number.longValue();
        }

        try {
            return Long.parseLong(userIdClaim.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void sendUnauthorizedResponse(
            HttpServletResponse response,
            String message) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");

        response.getWriter().write(
                """
                {
                    "success": false,
                    "message": "%s"
                }
                """.formatted(message)
        );
    }
}