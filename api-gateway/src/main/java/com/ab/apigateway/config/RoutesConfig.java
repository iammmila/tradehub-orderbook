package com.ab.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoutesConfig {

    @Bean
    public RouteLocator customRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-all", r -> r.path(
                                "/api/v1/auth/**",
                                "/api/v1/users/**",
                                "/api/v1/admin/**"
                        )
                        .uri("lb://AUTH-SERVICE"))
                .route("orders-all", r -> r.path(
                                "/api/v1/orders/**",
                                "/api/v1/orderbook/**"
                        )
                        .uri("lb://ORDER-SERVICE"))
                .route("trades-all", r -> r.path("/api/v1/trades/**")
                        .uri("lb://TRADE-SERVICE"))
                .build();
    }
}