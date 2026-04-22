package com.stockpro.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JwtUtil - handles all JWT token operations.
 *
 * What is a JWT?
 * JWT = JSON Web Token. It has 3 parts: Header.Payload.Signature
 * Header: algorithm used (HS256)
 * Payload: claims (userId, email, role, expiry)
 * Signature: proves the token wasn't tampered with
 *
 * IMPORTANT: jjwt 0.12.x API is different from 0.11.x
 * Old (0.11.x): Jwts.parserBuilder().setSigningKey(key).build()
 * New (0.12.x): Jwts.parser().verifyWith(key).build()
 * This file uses the 0.12.x API correctly.
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    // jwt.expiration in milliseconds (28800000 = 8 hours)
    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * Builds the signing key from the secret string.
     * The key must be at least 256 bits (32 characters) for HS256.
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate a JWT token for a successfully authenticated user.
     *
     * Claims stored in the token:
     *   subject  = email (standard JWT claim)
     *   userId   = user's database ID
     *   role     = STAFF / MANAGER / OFFICER / ADMIN
     *
     * The api-gateway reads these claims to know who is making each request.
     */
    public String generateToken(int userId, String email, String role) {
        return Jwts.builder()
                .subject(email)                          // standard "sub" claim
                .claim("userId", userId)                 // custom claim
                .claim("role", role)                     // custom claim
                .issuedAt(new Date())                    // when token was created
                .expiration(new Date(System.currentTimeMillis() + expiration))  // when it expires
                .signWith(getSigningKey())               // sign with our secret key
                .compact();                              // build the final token string
    }

    /**
     * Extract all claims (payload data) from a token.
     * Throws JwtException if token is invalid or expired.
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()                  // jjwt 0.12.x: use parser() not parserBuilder()
                .verifyWith(getSigningKey())  // jjwt 0.12.x: use verifyWith() not setSigningKey()
                .build()
                .parseSignedClaims(token)     // jjwt 0.12.x: use parseSignedClaims() not parseClaimsJws()
                .getPayload();
    }

    /**
     * Extract just the email (subject) from a token.
     */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extract the role from a token.
     */
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    /**
     * Extract the userId from a token.
     */
    public int extractUserId(String token) {
        return extractAllClaims(token).get("userId", Integer.class);
    }

    /**
     * Check if a token is expired.
     */
    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    /**
     * Validate a token - returns true if token is valid and not expired.
     * Returns false if token is invalid, expired, or tampered.
     */
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token); // throws exception if invalid
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}