package com.ab.orderservice.security;

/**
 * Authenticated user data carried in Spring SecurityContext.
 */
public record AuthPrincipal(String username, Long userId, boolean verified) {
}