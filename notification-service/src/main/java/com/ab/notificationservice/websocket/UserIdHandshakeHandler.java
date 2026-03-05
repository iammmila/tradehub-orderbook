package com.ab.notificationservice.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * Usage:
 * - Converts wsUserId attribute from the handshake into the STOMP Principal.
 * - This enables convertAndSendToUser(userId, ...) routing.
 */
@Component
public class UserIdHandshakeHandler extends DefaultHandshakeHandler {
    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        String userId = (String) attributes.get("wsUserId");
        return () -> userId; // Principal name = "2"
    }
}
