package com.orderservice.security;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class FeignClientConfig {

    @Autowired
    private ServiceJwtUtil serviceJwtUtil;

    @Bean
    public RequestInterceptor requestInterceptor() {

        return requestTemplate -> {

            Authentication authentication =
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication();

            /*
             * Normal HTTP request.
             *
             * Forward the customer's JWT.
             */
            if (authentication != null) {

                Object credentials =
                        authentication.getCredentials();

                if (credentials instanceof String token
                        && !token.isBlank()) {

                    requestTemplate.header(
                            "Authorization",
                            "Bearer " + token
                    );

                    return;
                }
            }

            /*
             * No HTTP SecurityContext.
             *
             * This normally happens when the Feign call
             * originates from a Kafka listener.
             *
             * Use an internal Order Service JWT.
             */
            String serviceToken =
                    serviceJwtUtil.generateServiceToken();

            requestTemplate.header(
                    "Authorization",
                    "Bearer " + serviceToken
            );
        };
    }
}