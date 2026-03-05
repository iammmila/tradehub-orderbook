package com.ab.notificationservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;

/**
 * Usage:
 * - Small JWT utility focused on extracting the subject (username) and normalizing token input.
 * - Shared by HTTP authentication filter and WebSocket handshake interceptor.
 */
@Service
public class JwtService {

    private final Key key;

    /**
     * Secret is injected from configuration and converted into an HMAC signing key.
     * Must be long enough for the chosen algorithm (jjwt enforces minimum sizes).
     */
    public JwtService(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Parses the JWT and returns the subject (username).
     * Throws JwtException/ExpiredJwtException when token is invalid or expired.
     */
    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Accepts either "Bearer <token>" or raw token and returns the raw token.
     * Returns null for missing header/token.
     */
    public String extractToken(String authHeader) {
        if (authHeader == null) return null;
        if (authHeader.startsWith("Bearer ")) return authHeader.substring(7);
        return authHeader;
    }
}