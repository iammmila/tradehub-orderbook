package com.ab.notificationservice.websocket;

import com.ab.notificationservice.security.JwtService;
import com.ab.notificationservice.security.UserIdResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WsJwtChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserIdResolver userIdResolver;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null) authHeader = accessor.getFirstNativeHeader("authorization");
            String token = jwtService.extractToken(authHeader);

            if (token == null || token.isBlank()) {
                throw new IllegalArgumentException("Missing Authorization header for WebSocket CONNECT");
            }

            String username = jwtService.extractUsername(token);

            // Resolve userId the same way you do in REST
            Long userId = userIdResolver.resolveUserId(username, token);

            // IMPORTANT: use userId as principal name (convertAndSendToUser uses this)
            Principal principal = new UsernamePasswordAuthenticationToken(
                    userId.toString(),
                    null,
                    List.of()
            );

            accessor.setUser(principal);
            log.info("WS CONNECT username={} userId={} principalName={}",
                    username, userId, principal.getName());
        }

        return message;
    }
}