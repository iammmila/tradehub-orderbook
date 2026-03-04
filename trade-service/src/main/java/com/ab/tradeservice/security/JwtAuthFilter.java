package com.ab.tradeservice.security;

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
 * Stateless authentication filter that reads JWT from Authorization header.
 * Sets AuthPrincipal into SecurityContext for downstream access control.
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

        // skip quickly when no token is present (public endpoints / unauthenticated calls)
        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String username = jwtService.extractUsername(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // userId is resolved once and stored in principal for later use (controllers/specifications)
                Long userId = userIdResolver.resolveUserId(username, token);

                AuthPrincipal principal = new AuthPrincipal(username, userId);

                var auth = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of() // no role-based authorities in this service (yet)
                );

                SecurityContextHolder.getContext().setAuthentication(auth);
            }

        } catch (ExpiredJwtException e) {
            // explicit 401 helps the UI trigger re-login/refresh flow
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"JWT expired. Please login again.\"}");
            return;
        } catch (JwtException e) {
            // invalid signature/malformed token -> treat as unauthenticated
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Invalid JWT.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}