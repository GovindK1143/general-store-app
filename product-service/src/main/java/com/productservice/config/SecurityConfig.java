package com.productservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests()
            .requestMatchers("/products/all", "/products/{category}").permitAll()  // Public access
            .requestMatchers("/products/add").authenticated() // Only authenticated users can add
            .and()
            .httpBasic(); // Enables basic authentication

        return http.build();
    }
}
