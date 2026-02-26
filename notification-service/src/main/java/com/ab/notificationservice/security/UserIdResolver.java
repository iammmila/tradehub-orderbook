package com.ab.notificationservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class UserIdResolver {

    private final WebClient.Builder webClientBuilder;

    @Value("${app.auth.base-url}")
    private String authBaseUrl;

    // Optional: cache (but careful: if usernames can change, clear cache)
    private final ConcurrentHashMap<String, Long> cache = new ConcurrentHashMap<>();

    public Long resolveUserId(String username, String bearerToken) {
        return cache.computeIfAbsent(username, u ->
                webClientBuilder.build()
                        .get()
                        .uri(authBaseUrl + "/api/v1/users/by-username/{username}", u)
                        .header("Authorization", "Bearer " + bearerToken)
                        .retrieve()
                        .bodyToMono(UserDto.class)
                        .map(UserDto::id)
                        .block()
        );
    }

    public record UserDto(Long id, String username) {}
}