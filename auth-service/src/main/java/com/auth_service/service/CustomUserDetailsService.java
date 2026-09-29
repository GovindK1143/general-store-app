package com.auth_service.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.auth_service.model.User;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AuthUserCacheService authUserCacheService;

    public CustomUserDetailsService(
            AuthUserCacheService authUserCacheService) {

        this.authUserCacheService = authUserCacheService;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = authUserCacheService.findUserByEmail(username);

        if (user == null) {
            throw new UsernameNotFoundException(
                    "User not found: " + username
            );
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(
                        new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().name()
                        )
                )
                .build();
    }
}