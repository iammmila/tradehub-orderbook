package com.ab.apigateway.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced// enables calling services by name (Eureka) like http://auth-service/...
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}
