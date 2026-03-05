package com.ab.notificationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Usage:
 * - Shared WebClient builder for outbound HTTP calls from this service.
 * - Lets you centralize timeouts, base URLs, codecs, interceptors, and auth headers later.
 */
@Configuration
public class WebClientConfig {
    /**
     * Exposes WebClient.Builder so services can create clients with their own baseUrl/config.
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}