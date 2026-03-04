package com.ab.tradeservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;

/**
 * JWT utility for extracting claims used by this service.
 * Keeps token parsing logic in one place for filters/controllers.
 */
@Service
public class JwtService {

    private final Key key;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        // construct the signing key once to avoid re-creating it per request
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String extractUsername(String token) {
        // parse and validate signature; returns the "sub" (subject) as username
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String extractToken(String authHeader) {
        // supports both "Bearer <token>" and raw token formats
        if (authHeader == null) return null;
        if (authHeader.startsWith("Bearer ")) return authHeader.substring(7);
        return authHeader;
    }
}