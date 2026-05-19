package com.stockpro.auth.service.impl;

import com.stockpro.auth.service.JwtBlacklistService;
import com.stockpro.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisJwtBlacklistService implements JwtBlacklistService {

    private static final String BLACKLIST_PREFIX = "stockpro:auth:jwt:blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    @Override
    public void blacklist(String token) {
        try {
            Date expiration = jwtUtil.extractExpiration(token);
            long ttlMillis = expiration.getTime() - System.currentTimeMillis();

            if (ttlMillis <= 0) {
                return;
            }

            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + tokenFingerprint(token),
                    "revoked",
                    Duration.ofMillis(ttlMillis)
            );
        } catch (Exception exception) {
            log.warn("JWT blacklist write failed. Logout continues without Redis persistence: {}", exception.getMessage());
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + tokenFingerprint(token)));
        } catch (Exception exception) {
            log.warn("JWT blacklist lookup failed. Falling back to signature validation only: {}", exception.getMessage());
            return false;
        }
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
}
