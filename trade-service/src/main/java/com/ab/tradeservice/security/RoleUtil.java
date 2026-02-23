package com.ab.tradeservice.security;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class RoleUtil {

    private RoleUtil() {
    }

    public static boolean hasRole(String rolesHeader, String role) {
        return parse(rolesHeader).contains(role);
    }

    public static boolean hasAnyRole(String rolesHeader, String... roles) {
        Set<String> userRoles = parse(rolesHeader);
        for (String r : roles) {
            if (userRoles.contains(r)) return true;
        }
        return false;
    }

    private static Set<String> parse(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) return Set.of();
        return Arrays
                .stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }
}