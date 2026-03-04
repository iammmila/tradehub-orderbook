package com.ab.tradeservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Shared WebClient builder for outbound HTTP calls.
 *
 * Usage:
 * - Inject WebClient.Builder where you call other services (auth-service, order-service, etc.).
 * - Using a Builder allows per-call customization (baseUrl, headers, filters) without recreating the client.
 */
@Configuration
public class WebClientConfig {
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
