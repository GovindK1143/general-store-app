package com.orderservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthFilter jwtAuthFilter) throws Exception {

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
                        // CUSTOMER + ADMIN
                        // ==========================================
                        .requestMatchers(
                                HttpMethod.POST,
                                "/orders/place"
                        ).hasAnyRole("CUSTOMER", "ADMIN")

                        // ==========================================
                        // ADMIN ONLY
                        // ==========================================
                        .requestMatchers(
                                HttpMethod.GET,
                                "/orders/all"
                        ).hasRole("ADMIN")

                        // ==========================================
                        // CUSTOMER + ADMIN
                        // ==========================================
                        .requestMatchers(
                                HttpMethod.GET,
                                "/orders/user/**"
                        ).hasAnyRole("CUSTOMER", "ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/orders/*/status"
                        ).hasAnyRole("CUSTOMER", "ADMIN")

                        // ==========================================
                        // OTHER ORDER ENDPOINTS
                        // ==========================================
                        .requestMatchers("/orders/**")
                        .authenticated()

                        // ==========================================
                        // NON-ORDER ENDPOINTS
                        // ==========================================
                        .anyRequest()
                        .permitAll()
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