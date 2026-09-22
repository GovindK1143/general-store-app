package com.paymentservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.paymentservice.security.JwtAuthFilter;

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

                        .requestMatchers("/actuator/health")
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/payments/process"
                        )
                        .hasAnyRole("CUSTOMER", "ADMIN")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/payments/createPending"
                        )
                        .hasAnyRole("CUSTOMER", "ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/payments/order/**"
                        )
                        .hasAnyRole("CUSTOMER", "ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/payments/status/**"
                        )
                        .hasAnyRole("CUSTOMER", "ADMIN")

                        .anyRequest()
                        .authenticated()
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