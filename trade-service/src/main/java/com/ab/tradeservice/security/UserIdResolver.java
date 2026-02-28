package com.ab.tradeservice.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserIdResolver {

    private final WebClient.Builder webClientBuilder;

    @Value("${app.auth.base-url}")
    private String authBaseUrl;

    private final ConcurrentHashMap<String, Long> cache = new ConcurrentHashMap<>();

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
                        .timeout(Duration.ofSeconds(3))
                        .block();
            } catch (Exception ex) {
                log.error("Failed to resolve userId for username={}: {}", username, ex.getMessage());
                throw new IllegalStateException("Cannot resolve userId: " + username, ex);
            }
        });
    }

    public record UserDto(Long id, String username) {}
}