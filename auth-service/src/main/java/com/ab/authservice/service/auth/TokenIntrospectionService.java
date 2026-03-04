package com.ab.authservice.service.auth;

import com.ab.authservice.dto.auth.IntrospectResponse;
import com.ab.authservice.exception.UnauthorizedException;
import com.ab.authservice.exception.enums.ErrorCode;
import com.ab.authservice.jwt.JwtService;
import com.ab.authservice.model.User;
import com.ab.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TokenIntrospectionService {
    private final JwtService jwtService;
    private final UserRepository userRepository;

    // GET /api/v1/auth/introspect -> 200 OK (or 401)
    // Used by API Gateway: validates JWT and returns user id + roles.
    public IntrospectResponse introspect(String authHeader) {
        // 1) Extract raw token string from "Bearer <token>"
        String token = jwtService.extractToken(authHeader);
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
