package com.ab.orderservice.auth.service;

import com.ab.orderservice.auth.dto.AuthRequest;
import com.ab.orderservice.auth.dto.AuthResponse;
import com.ab.orderservice.auth.dto.RegisterRequest;
import com.ab.orderservice.auth.jwt.JwtService;
import com.ab.orderservice.auth.model.Role;
import com.ab.orderservice.auth.model.User;
import com.ab.orderservice.auth.repository.RoleRepository;
import com.ab.orderservice.auth.repository.UserRepository;
import com.ab.orderservice.auth.userdetails.CustomUserDetailsService;
import com.ab.orderservice.common.exception.BadRequestException;
import com.ab.orderservice.common.exception.NotFoundException;
import com.ab.orderservice.common.exception.UnauthorizedException;
import com.ab.orderservice.common.exception.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

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
                .password(passwordEncoder.encode(request.getPassword()))
                .role(userRole)
                .build();

        userRepository.save(user);
    }
}
