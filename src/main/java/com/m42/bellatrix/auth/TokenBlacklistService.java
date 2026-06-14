package com.m42.bellatrix.auth;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TokenBlacklistService {
    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void blacklist(String tokenId, Duration ttl) {
        try {
            redisTemplate.opsForValue().set("auth:blacklist:" + tokenId, "1", ttl);
        } catch (Exception ignored) {
        }
    }

    public boolean isBlacklisted(String tokenId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey("auth:blacklist:" + tokenId));
        } catch (Exception ignored) {
            return false;
        }
    }
}
