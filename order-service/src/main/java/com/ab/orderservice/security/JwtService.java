package com.ab.orderservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    private SecretKey key;

    @PostConstruct
    void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return claims(token).getSubject();
    }

    public Long extractUserId(String token) {
        Object v = claims(token).get("uid");
        if (v == null) return null;

        if (v instanceof Long l) return l;
        if (v instanceof Integer i) return i.longValue();
        if (v instanceof String s) return Long.valueOf(s);

        throw new IllegalStateException("Invalid uid claim type: " + v.getClass());
    }

    public boolean extractVerified(String token) {
        Object v = claims(token).get("verified");
        if (v == null) return false;

        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return Boolean.parseBoolean(s);

        throw new IllegalStateException("Invalid verified claim type: " + v.getClass());
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object v = claims(token).get("roles");
        if (v == null) return List.of();
        if (v instanceof List<?> list) return (List<String>) list;
        return List.of();
    }

    public String extractToken(String authHeader) {
        if (authHeader == null) return null;
        if (authHeader.startsWith("Bearer ")) return authHeader.substring(7);
        return authHeader;
    }
}
/**
 * subject = username
 */
//    public String extractUsername(String token) {
//        return Jwts.parser()
//                .verifyWith((javax.crypto.SecretKey) key)
//                .build()
//                .parseSignedClaims(token)
//                .getPayload()
//                .getSubject();
//    }
//
//    public String extractToken(String authHeader) {
//        if (authHeader == null) return null;
//        if (authHeader.startsWith("Bearer ")) return authHeader.substring(7);
//        return authHeader;
//    }
//}