package com.ab.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    @Bean  // BCrypt is the standard safe password hashing algorithm for Spring apps
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
