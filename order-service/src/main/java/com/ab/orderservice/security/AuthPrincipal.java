package com.ab.orderservice.security;

public record AuthPrincipal(String username, Long userId, boolean verified) {
}