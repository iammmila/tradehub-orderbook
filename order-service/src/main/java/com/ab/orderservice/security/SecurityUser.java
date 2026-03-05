package com.ab.orderservice.security;

import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Convenience accessors for current authenticated user.
 */
public final class SecurityUser {
    private SecurityUser() {}

    // Current AuthPrincipal from SecurityContext (or null).
    public static AuthPrincipal principal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        if (auth.getPrincipal() instanceof AuthPrincipal p) return p;
        return null;
    }

    // Current userId claim (or null).
    public static Long userId() {
        var p = principal();
        return p != null ? p.userId() : null;
    }

    // Current verified claim (default false).
    public static boolean verified() {
        var p = principal();
        return p != null && p.verified();
    }
}