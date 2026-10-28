package com.productservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth

                        // ==========================================
                        // SWAGGER / OPENAPI
                        // ==========================================

                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // ==========================================
                        // PUBLIC PRODUCT CATALOGUE
                        // GUEST + CUSTOMER + ADMIN
                        // ==========================================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/products/all",
                                "/products/categories",
                                "/products/featured",
                                "/products/id/**",
                                "/products/category/**",
                                "/products/search"
                        ).permitAll()

                        // ADMIN ONLY - VIEW ALL PRODUCTS, INCLUDING INACTIVE
                        .requestMatchers(
                                HttpMethod.GET,
                                "/products/admin/all"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/products/add/bulk"
                        ).hasRole("ADMIN")

                        // ==========================================
                        // ADMIN ONLY - ADD PRODUCT
                        // ==========================================

                        .requestMatchers(
                                HttpMethod.POST,
                                "/products/add"
                        ).hasRole("ADMIN")

                        // ==========================================
                        // ADMIN ONLY - UPDATE STOCK
                        // ==========================================

                        .requestMatchers(
                                HttpMethod.POST,
                                "/products/update-stock",
                                "/products/update-stock/batch"
                        ).hasRole("ADMIN")

                        // ==========================================
                        // ADMIN ONLY - UPDATE PRODUCT
                        // ==========================================

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/products/*"
                        ).hasRole("ADMIN")

                        // ==========================================
                        // ADMIN ONLY - ACTIVATE / DEACTIVATE PRODUCT
                        // ==========================================

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/products/*/status"
                        ).hasRole("ADMIN")

                        // ==========================================
                        // EVERYTHING ELSE REQUIRES AUTHENTICATION
                        // ==========================================

                        .anyRequest().authenticated()
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}