package com.ab.notificationservice.security;

/**
 * Usage:
 * - Lightweight authenticated user representation stored in Spring Security context.
 * - Exposed to controllers via @AuthenticationPrincipal to scope operations by userId.
 */
public record AuthPrincipal(String username, Long userId) {}