package com.ab.orderservice.security;

import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUser {
    private SecurityUser() {}

    public static Long userId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        if (auth.getPrincipal() instanceof AuthPrincipal p) return p.userId();
        return null;
    }
}
