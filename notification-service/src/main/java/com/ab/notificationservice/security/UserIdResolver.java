package com.ab.notificationservice.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Usage:
 * - Resolves (username -> userId) by calling auth-service.
 * - Used by both HTTP JWT filter and WebSocket handshake auth to scope notifications by userId.
 * - Includes a small in-memory cache to reduce repeated network calls.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserIdResolver {

    private final WebClient.Builder webClientBuilder;

    @Value("${app.auth.base-url}")
    private String authBaseUrl;

    /**
     * Cache key is username; value is userId.
     * Useful for frequent requests, but assumes username->userId mapping is stable.
     */
    private final ConcurrentHashMap<String, Long> cache = new ConcurrentHashMap<>();

    /**
     * Calls auth-service endpoint to fetch userId for the given username.
     * Forwards the same JWT to keep auth-service authorization consistent.
     * Timeout prevents thread starvation when auth-service is slow/unavailable.
     */
    public Long resolveUserId(String username, String bearerToken) {
        return cache.computeIfAbsent(username, u -> {
                    try {
                        return webClientBuilder.build()
                                .get()
                                .uri(authBaseUrl + "/api/v1/users/by-username/{username}", u)
                                .header("Authorization", "Bearer " + bearerToken)
                                .retrieve()
                                .bodyToMono(UserDto.class)
                                .map(UserDto::id)
                                .timeout(Duration.ofSeconds(3))  // ← add timeout
                                .block();
                    } catch (Exception ex) {
                        log.error("Failed to resolve userId for username={}: {}", username, ex.getMessage());
                        throw new IllegalStateException("Cannot resolve userId for WebSocket auth: " + username, ex);
                    }
                }
        );
    }

    /**
     * Minimal DTO for auth-service response.
     * Only id is required for scoping; username is kept for compatibility/debugging.
     */
    public record UserDto(Long id, String username) {
    }
}