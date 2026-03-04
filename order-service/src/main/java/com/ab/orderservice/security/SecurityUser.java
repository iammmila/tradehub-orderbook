package com.ab.orderservice.security;

import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUser {
    private SecurityUser() {}

    public static AuthPrincipal principal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        if (auth.getPrincipal() instanceof AuthPrincipal p) return p;
        return null;
    }

    public static Long userId() {
        var p = principal();
        return p != null ? p.userId() : null;
    }

    public static boolean verified() {
        var p = principal();
        return p != null && p.verified();
    }
}