package com.ab.orderservice.auth.service;

import com.ab.orderservice.auth.dto.RegisterRequest;
import com.ab.orderservice.auth.model.Role;
import com.ab.orderservice.auth.model.User;
import com.ab.orderservice.auth.repository.RoleRepository;
import com.ab.orderservice.auth.repository.UserRepository;
import com.ab.orderservice.common.exception.BadRequestException;
import com.ab.orderservice.common.exception.NotFoundException;
import com.ab.orderservice.common.exception.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException(ErrorCode.USER_USERNAME_ALREADY_EXISTS);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException(ErrorCode.USER_EMAIL_ALREADY_EXISTS);
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new NotFoundException(ErrorCode.ROLE_NOT_FOUND));

        User user = User.builder()
                .username(request.getUsername().trim())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(userRole)
                .build();

        userRepository.save(user);
    }
}
