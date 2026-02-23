package com.ab.authservice.api;

import com.ab.authservice.dto.AuthRequest;
import com.ab.authservice.dto.AuthResponse;
import com.ab.authservice.dto.IntrospectResponse;
import com.ab.authservice.dto.RegisterRequest;
import com.ab.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    //Post /api/v1/auth/login -> 200 ok
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    //Post /api/v1/auth/register -> 200 ok
    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequest request) {

        authService.register(request);
        return ResponseEntity.ok("User registered successfully");
    }

    // Used by API Gateway to validate the JWT and extract user identity.
    // Gateway calls this on every protected request and then forwards X-User-Id / X-Roles headers.
    //get /api/v1/auth/introspect -> 200 ok
    @GetMapping("/introspect")
    public ResponseEntity<IntrospectResponse> introspect(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        return ResponseEntity.ok(authService.introspect(authorization));
    }
}
