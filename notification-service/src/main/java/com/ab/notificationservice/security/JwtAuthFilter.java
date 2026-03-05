package com.ab.notificationservice.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Usage:
 * - Reads JWT from Authorization header for every HTTP request.
 * - Builds an AuthPrincipal (username + userId) and stores it in the SecurityContext.
 * - If JWT is invalid/expired, returns 401 and stops the request chain.
 */
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserIdResolver userIdResolver;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = jwtService.extractToken(authHeader);

        // No token means "anonymous" here; security rules will decide access.
        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 1) extract username (sub) from JWT
            String username = jwtService.extractUsername(token);

            // 2) if not authenticated yet, build Authentication
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 3) resolve userId by calling auth-service/gateway (FORWARD SAME JWT)
                Long userId = userIdResolver.resolveUserId(username, token);

                // 4) Principal used by controllers via @AuthenticationPrincipal
                AuthPrincipal principal = new AuthPrincipal(username, userId);

                var auth = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of()
                );

                SecurityContextHolder.getContext().setAuthentication(auth);
            }

        } catch (ExpiredJwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"JWT expired. Please login again.\"}");
            return;
        } catch (JwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Invalid JWT.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}