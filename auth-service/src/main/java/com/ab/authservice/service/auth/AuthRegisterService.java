package com.ab.authservice.service.auth;

import com.ab.authservice.dto.RegisterRequest;
import com.ab.authservice.exception.BadRequestException;
import com.ab.authservice.exception.NotFoundException;
import com.ab.authservice.exception.enums.ErrorCode;
import com.ab.authservice.model.Role;
import com.ab.authservice.model.User;
import com.ab.authservice.model.enums.AuthProvider;
import com.ab.authservice.repository.RoleRepository;
import com.ab.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthRegisterService {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // POST /api/v1/auth/register -> 201 Created (controller)
    public void register(RegisterRequest request) {

        // Uniqueness checks
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException(ErrorCode.USER_USERNAME_ALREADY_EXISTS);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException(ErrorCode.USER_EMAIL_ALREADY_EXISTS);
        }

        // Default role for new users
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new NotFoundException(ErrorCode.ROLE_NOT_FOUND));

        // Create LOCAL user (password stored as BCrypt hash)
        User user = User.builder()
                .username(request.getUsername().trim())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail().trim().toLowerCase())
                .provider(AuthProvider.LOCAL)
                .providerId(null)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(userRole)
                .build();

        userRepository.save(user);
    }
}