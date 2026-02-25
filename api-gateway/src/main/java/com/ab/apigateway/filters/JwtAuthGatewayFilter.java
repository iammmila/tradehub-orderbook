package com.ab.apigateway.filters;

import com.ab.apigateway.dto.IntrospectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthGatewayFilter implements GlobalFilter {

    private final WebClient.Builder webClientBuilder;

    // Paths that should NOT require auth
    private boolean isPublicPath(String path) {
        return path.startsWith("/api/v1/auth/"); // login/register/introspect etc.
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        //  Always allow CORS preflight requests
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        // Public endpoints pass through
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Call auth-service introspection
        return webClientBuilder.build()
                .get()
                .uri("http://auth-service/api/v1/auth/introspect")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .retrieve()
                .bodyToMono(IntrospectResponse.class)
                .flatMap(payload -> {
                    if (payload == null || payload.getUserId() == null) {
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }

                    String rolesHeader = joinRoles(payload.getRoles());

                    ServerHttpRequest mutatedRequest = exchange.getRequest()
                            .mutate()
                            .header("X-User-Id", String.valueOf(payload.getUserId()))
                            .header("X-Username", payload.getUsername() == null ? "" : payload.getUsername())
                            .header("X-Roles", rolesHeader)
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .onErrorResume(ex -> {
                    // token invalid, auth-service down, parsing error, etc.
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                });
    }

    private String joinRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) return "";
        return String.join(",", roles);
    }
}