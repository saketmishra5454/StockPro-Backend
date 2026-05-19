package com.stockpro.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String secret;

    @Autowired(required = false)
    private ReactiveStringRedisTemplate redisTemplate;

    private static final String BLACKLIST_PREFIX = "stockpro:auth:jwt:blacklist:";

    private static final List<String> WHITE_LIST = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/refresh"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        // Authentication endpoints are public; all other requests require JWT validation
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return sendUnauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = validateToken(token);

            String userId = String.valueOf(claims.get("userId"));
            String email  = claims.getSubject();
            String role   = String.valueOf(claims.get("role"));

            return isTokenBlacklisted(token)
                    .flatMap(blacklisted -> {
                        if (blacklisted) {
                            return sendUnauthorized(exchange, "Token has been revoked");
                        }

                        // Identity headers let downstream services avoid parsing JWTs again
                        ServerHttpRequest modifiedRequest = request.mutate()
                                .header("X-User-Id",    userId)
                                .header("X-User-Email", email)
                                .header("X-User-Role",  role)
                                .build();

                        return chain.filter(exchange.mutate().request(modifiedRequest).build());
                    });

        } catch (ExpiredJwtException e) {
            return sendUnauthorized(exchange, "Token has expired");

        } catch (SignatureException e) {
            return sendUnauthorized(exchange, "Invalid token signature");

        } catch (MalformedJwtException e) {
            return sendUnauthorized(exchange, "Malformed token");

        } catch (Exception e) {
            return sendUnauthorized(exchange, "Token validation failed");
        }
    }


    private Claims validateToken(String token) {
        Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    private boolean isWhitelisted(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    private Mono<Boolean> isTokenBlacklisted(String token) {
        if (redisTemplate == null) {
            return Mono.just(false);
        }

        return redisTemplate.hasKey(BLACKLIST_PREFIX + tokenFingerprint(token))
                .onErrorResume(exception -> Mono.just(false));
    }

    private String tokenFingerprint(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JWT fingerprint hashing is unavailable.", exception);
        }
    }

    private Mono<Void> sendUnauthorized(ServerWebExchange exchange, String reason) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("X-Auth-Error", reason);
        return response.setComplete();
    }


    @Override
    public int getOrder() {
        return -1;
    }
}
