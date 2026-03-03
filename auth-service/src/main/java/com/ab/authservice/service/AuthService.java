package com.ab.authservice.service;

import com.ab.authservice.dto.AuthRequest;
import com.ab.authservice.dto.AuthResponse;
import com.ab.authservice.dto.IntrospectResponse;
import com.ab.authservice.dto.RegisterRequest;
import com.ab.authservice.jwt.JwtService;
import com.ab.authservice.model.Role;
import com.ab.authservice.model.User;
import com.ab.authservice.model.enums.AuthProvider;
import com.ab.authservice.repository.RoleRepository;
import com.ab.authservice.repository.UserRepository;
import com.ab.authservice.userdetails.CustomUserDetailsService;
import com.ab.authservice.exception.BadRequestException;
import com.ab.authservice.exception.NotFoundException;
import com.ab.authservice.exception.UnauthorizedException;
import com.ab.authservice.exception.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtService jwtService;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            // wrong username or password
            throw new UnauthorizedException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        } catch (AuthenticationException e) {
            // any other auth-related failure
            throw new UnauthorizedException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        UserDetails user = customUserDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtService.generateToken(user);
        return new AuthResponse(token);
    }

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
                .provider(AuthProvider.LOCAL)
                .providerId(null)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(userRole)
                .build();

        userRepository.save(user);
    }

    public IntrospectResponse introspect(String authHeader) {
        // 1) Extract raw token string from "Bearer <token>"
        String token = jwtService.extractToken(authHeader);

        // 2) Extract raw token string from "Bearer <token>"
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        // 3) Parse token and read username (subject). If parsing fails => invalid/expired token
        String username;
        try {
            username = jwtService.extractUsername(token);
        } catch (Exception e) {
            throw new UnauthorizedException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        // 4) Confirm the user still exists in DB (token could be for deleted user)
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.USER_NOT_FOUND));

        // 5) Convert role to a list because gateway forwards it as "X-Roles"
        List<String> roles = List.of(user.getRole().getName());

        // 6) Return identity details to gateway
        // Gateway will attach these to downstream calls as headers:
        // X-User-Id, X-Username, X-Roles
        return IntrospectResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .roles(roles)
                .build();
    }
}
