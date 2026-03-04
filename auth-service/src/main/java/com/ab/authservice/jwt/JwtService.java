package com.ab.authservice.jwt;

import com.ab.authservice.userdetails.CustomUserDetails;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;

// Creates and validates JWT tokens using a secret from configuration (ENV).
@Service
@RequiredArgsConstructor
public class JwtService {
    @Value("${security.jwt.secret}")
    private String secret;

    // Builds signing key from secret
    private Key signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Creates JWT that includes uid/verified/roles for other services (via gateway introspect or direct parsing)
    public String generateToken(UserDetails userDetails) {
        if (!(userDetails instanceof CustomUserDetails cud)) {
            throw new IllegalStateException("UserDetails must be CustomUserDetails to include uid/verified claims");
        }

        Long uid = cud.getId();
        boolean verified = cud.getUser().isVerified();
        List<String> roles = cud
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim("uid", uid)
                .claim("verified", verified)
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Validates token signature/expiry and returns username (subject)
    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // Helper: extracts raw token from Authorization header
    public String extractToken(String authHeader) {
        if (authHeader == null) return null;
        if (authHeader.startsWith("Bearer ")) return authHeader.substring(7);
        return authHeader;
    }
}
