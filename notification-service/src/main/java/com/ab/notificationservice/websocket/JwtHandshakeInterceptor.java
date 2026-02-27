package com.ab.notificationservice.websocket;

import com.ab.notificationservice.security.JwtService;
import com.ab.notificationservice.security.UserIdResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final UserIdResolver userIdResolver;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        URI uri = request.getURI();
        String query = uri.getQuery(); // token=...

        String token = null;
        if (query != null) {
            for (String part : query.split("&")) {
                if (part.startsWith("token=")) {
                    token = URLDecoder.decode(part.substring(6), StandardCharsets.UTF_8);
                    break;
                }
            }
        }

        token = jwtService.extractToken(token); // supports "Bearer ..."
        if (token == null || token.isBlank()) {
            log.warn("WS handshake rejected: missing token");
            return false;
        }

        String username = jwtService.extractUsername(token);
        Long userId = userIdResolver.resolveUserId(username, token);

        attributes.put("wsUserId", userId.toString());
        log.debug("WS HANDSHAKE OK username={} userId={}", username, userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
