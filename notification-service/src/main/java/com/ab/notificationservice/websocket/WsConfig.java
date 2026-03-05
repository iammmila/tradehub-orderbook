package com.ab.notificationservice.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Usage:
 * - WebSocket/STOMP configuration for live notification delivery.
 * - Clients connect to /ws and subscribe to /user/queue/notifications.
 * - Handshake interceptor extracts JWT and assigns Principal name = userId.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WsConfig implements WebSocketMessageBrokerConfigurer {
    @Value("${app.ws.allowed-origins}")
    private String allowedOrigins;

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final UserIdHandshakeHandler userIdHandshakeHandler;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")   // SockJS helps when browser/network blocks native websockets
                .setAllowedOrigins(allowedOrigins)
                .addInterceptors(jwtHandshakeInterceptor)
                .setHandshakeHandler(userIdHandshakeHandler)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {// Simple in-memory broker is enough for dev/single instance
        registry.enableSimpleBroker("/queue");
        registry.setUserDestinationPrefix("/user");
        registry.setApplicationDestinationPrefixes("/app");
    }
}