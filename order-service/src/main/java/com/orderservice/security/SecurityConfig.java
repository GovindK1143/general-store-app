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

                        // CUSTOMER and ADMIN can place orders
                        .requestMatchers(
                                HttpMethod.POST,
                                "/orders/place"
                        ).hasAnyRole("CUSTOMER", "ADMIN")

                        // Only ADMIN can view all orders
                        .requestMatchers(
                                HttpMethod.GET,
                                "/orders/all"
                        ).hasRole("ADMIN")

                        // CUSTOMER and ADMIN can view user orders
                        .requestMatchers(
                                HttpMethod.GET,
                                "/orders/user/**"
                        ).hasAnyRole("CUSTOMER", "ADMIN")

                        // CUSTOMER and ADMIN can check order status
                        .requestMatchers(
                                HttpMethod.GET,
                                "/orders/*/status"
                        ).hasAnyRole("CUSTOMER", "ADMIN")

                        // Any other order endpoint requires authentication
                        .requestMatchers("/orders/**")
                        .authenticated()

                        // Non-order endpoints
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