package com.ab.notificationservice.config;

import com.ab.notificationservice.security.JwtAuthFilter;
import com.ab.notificationservice.security.JwtService;
import com.ab.notificationservice.security.UserIdResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Usage:
 * - Spring Security configuration for the notification-service API.
 * - Stateless JWT authentication via a custom filter.
 * - Allows actuator and websocket paths without auth (WS auth can be added later).
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final UserIdResolver userIdResolver;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/ws/**").permitAll() // WS auth later
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthFilter(jwtService, userIdResolver), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}