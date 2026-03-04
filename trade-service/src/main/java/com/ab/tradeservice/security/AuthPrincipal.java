package com.ab.tradeservice.security;

/**
 * Minimal authenticated identity stored in Spring Security context.
 * Carries userId to avoid repeating lookups in controllers/services.
 */
public record AuthPrincipal(String username, Long userId) {
}
