package com.jakstream.api_gateway.config;

import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthFilter implements GlobalFilter {

    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/auth/register",
        "/api/auth/login"
    );

    private final SecretKey key;

    public JwtAuthFilter(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Always strip internal-only headers so a malicious client can never
        // spoof an identity — even on public routes where no JWT is required.
        ServerHttpRequest sanitized = exchange.getRequest().mutate().headers(headers -> {
            headers.remove("user_id");
            headers.remove("user_role");
            headers.remove("X-User-Id");
            headers.remove("X-User-Role");
        }).build();
        exchange = exchange.mutate().request(sanitized).build();

        String path = sanitized.getURI().getPath();
        String method = sanitized.getMethod().name();

        if (isPublicPath(path, method)) {
            return chain.filter(exchange);
        }

        String authHeader = sanitized.getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        try {
            Jws<Claims> claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);

            String userId = claims.getPayload().getSubject();
            String role = claims.getPayload().get("role", String.class);

            ServerHttpRequest modifiedRequest = sanitized.mutate().headers(headers -> {
                headers.remove("Authorization");
                headers.add("user_id", userId);
                headers.add("user_role", role);
            }).build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isPublicPath(String path, String method) {
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            return true;
        }
        if (path.startsWith("/api/streams") && "GET".equals(method)) {
            return true;
        }
        // WebSocket upgrade requests — browsers cannot attach custom headers to
        // a WS handshake, so authentication is handled at the STOMP protocol
        // level inside chat-service (StompAuthInterceptor).
        if (path.startsWith("/ws/")) {
            return true;
        }
        return false;
    }
    
}
