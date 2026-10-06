package com.streaming.chat_service.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Validates JWT tokens on STOMP CONNECT frames.
 *
 * Browsers cannot attach custom headers to a raw WebSocket handshake, so the
 * API gateway lets {@code /ws/**} through without a Bearer token. Instead,
 * the client passes its JWT inside the STOMP-level {@code Authorization}
 * header (set via {@code connectHeaders} in the STOMP client library).
 *
 * On a valid token, the user's id and username are stored in the WebSocket
 * session attributes so downstream {@code @MessageMapping} handlers can
 * read them — the client payload is never trusted for identity.
 */
@Component
public class StompAuthInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompAuthInterceptor.class);

    private final SecretKey key;

    public StompAuthInterceptor(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new MessageDeliveryException("Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);

            try {
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String userId = claims.getSubject();
                String username = claims.get("username", String.class);

                var sessionAttrs = accessor.getSessionAttributes();
                if (sessionAttrs != null) {
                    sessionAttrs.put("userId", userId);
                    sessionAttrs.put("username", username);
                }

                log.debug("STOMP CONNECT authenticated: userId={}, username={}", userId, username);

            } catch (Exception e) {
                log.warn("STOMP CONNECT rejected: {}", e.getMessage());
                throw new MessageDeliveryException("Invalid or expired JWT token");
            }
        }

        return message;
    }
}
