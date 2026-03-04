package com.ab.authservice.service.auth;

import com.ab.authservice.dto.auth.AuthRequest;
import com.ab.authservice.dto.auth.AuthResponse;
import com.ab.authservice.exception.UnauthorizedException;
import com.ab.authservice.exception.enums.ErrorCode;
import com.ab.authservice.jwt.JwtService;
import com.ab.authservice.userdetails.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthLoginService {
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtService jwtService;

    // POST /api/v1/auth/login -> 200 OK (or 401)
    public AuthResponse login(AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
            // Avoid leaking whether username or password was wrong
            throw new UnauthorizedException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        // After successful authentication, generate JWT for the user
        UserDetails user = customUserDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtService.generateToken(user);
        return new AuthResponse(token);
    }
}
