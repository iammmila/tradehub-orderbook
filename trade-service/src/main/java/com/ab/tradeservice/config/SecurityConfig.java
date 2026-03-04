package com.ab.tradeservice.config;

import com.ab.tradeservice.security.JwtAuthFilter;
import com.ab.tradeservice.security.JwtService;
import com.ab.tradeservice.security.UserIdResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for Trade Service APIs.
 * <p>
 * Usage:
 * - Enforces JWT authentication for all business endpoints.
 * - Keeps the service stateless (no server-side sessions) which is the standard for microservices.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final UserIdResolver userIdResolver;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                //stateless APIs typically disable CSRF because they don't rely on cookies for auth.
                .csrf(csrf -> csrf.disable())
                //JWT-based auth should not create HTTP sessions; every request must carry its token.
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                // JWT filter must run before UsernamePasswordAuthenticationFilter so Spring Security
                // sees an authenticated principal early in the chain.
                .addFilterBefore(new JwtAuthFilter(jwtService, userIdResolver), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}