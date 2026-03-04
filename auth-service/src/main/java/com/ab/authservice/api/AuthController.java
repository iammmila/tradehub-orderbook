package com.ab.authservice.api;

import com.ab.authservice.dto.auth.AuthRequest;
import com.ab.authservice.dto.auth.AuthResponse;
import com.ab.authservice.dto.auth.IntrospectResponse;
import com.ab.authservice.dto.RegisterRequest;
import com.ab.authservice.service.auth.AuthLoginService;
import com.ab.authservice.service.auth.AuthRegisterService;
import com.ab.authservice.service.auth.TokenIntrospectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthLoginService authLoginService;
    private final AuthRegisterService authRegisterService;
    private final TokenIntrospectionService tokenIntrospectionService;

    // POST /api/v1/auth/login -> 200 OK (or 401 invalid credentials)
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authLoginService.login(request));
    }

    // POST /api/v1/auth/register -> 201 Created (better than 200)
    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequest request) {
        authRegisterService.register(request);
        return ResponseEntity.status(201).body("User registered successfully");
    }

    // GET /api/v1/auth/introspect -> 200 OK (or 401 invalid/expired token)
    // Used by API Gateway to validate JWT and extract user identity + roles.
    @GetMapping("/introspect")
    public ResponseEntity<IntrospectResponse> introspect(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        return ResponseEntity.ok(tokenIntrospectionService.introspect(authorization));
    }
}
