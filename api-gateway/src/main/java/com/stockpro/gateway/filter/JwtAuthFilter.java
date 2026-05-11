package com.stockpro.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
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
import java.util.List;

/**
 * JwtAuthFilter - runs BEFORE every request is forwarded to any microservice.
 *
 * What it does:
 * 1. Checks if the request path is in the whitelist (login, register) → allows through
 * 2. For all other paths → checks for "Authorization: Bearer <token>" header
 * 3. Validates the JWT token using the same secret as auth-service
 * 4. If valid → extracts userId, email, role from token and adds them as request headers
 *    so downstream services can use them without re-validating the token
 * 5. If invalid or missing → returns 401 Unauthorized immediately
 *
 * Why GlobalFilter + Ordered?
 * GlobalFilter means this runs for ALL routes automatically.
 * Ordered with getOrder() = -1 means this runs FIRST before any other filters.
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String secret;

    /**
     * Paths that do NOT need a JWT token.
     * Anyone can call login and register without being authenticated.
     */
    private static final List<String> WHITE_LIST = List.of(
            "/api/auth/login",
            "/api/auth/register"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        // Step 1: Check whitelist - if path is login or register, skip JWT check
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        // Step 2: Get Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // Step 3: Check header exists and starts with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return sendUnauthorized(exchange, "Missing or invalid Authorization header");
        }

        // Step 4: Extract the token (remove "Bearer " prefix)
        String token = authHeader.substring(7);

        try {
            // Step 5: Validate token and extract claims
            Claims claims = validateToken(token);

            // Step 6: Extract user info from token claims
            String userId = String.valueOf(claims.get("userId"));
            String email  = claims.getSubject();
            String role   = String.valueOf(claims.get("role"));

            // Step 7: Add user info as headers so downstream services can use them
            // This way each microservice knows WHO is making the request
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id",    userId)
                    .header("X-User-Email", email)
                    .header("X-User-Role",  role)
                    .build();

            // Step 8: Forward the modified request to the target microservice
            return chain.filter(exchange.mutate().request(modifiedRequest).build());

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

    /**
     * Validates the JWT token and returns its claims (payload data).
     * Uses the same secret key as auth-service so both can read the same tokens.
     */
    private Claims validateToken(String token) {
        Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Checks if the request path matches any whitelisted path.
     * Uses startsWith so /api/auth/login?redirect=x also passes through.
     */
    private boolean isWhitelisted(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    /**
     * Sends a 401 Unauthorized response and stops the request from going further.
     * The microservice never receives the request.
     */
    private Mono<Void> sendUnauthorized(ServerWebExchange exchange, String reason) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("X-Auth-Error", reason);
        return response.setComplete();
    }

    /**
     * Order -1 means this filter runs before all other gateway filters.
     * This is important — we want to reject bad requests immediately.
     */
    @Override
    public int getOrder() {
        return -1;
    }
}
