package com.example.chatservice.services;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class ProfileImageService {

    private final RedisTemplate<String, byte[]> redisTemplate;
    private static final String PROFILE_IMG_PREFIX = "profile_img:";

    public ProfileImageService(RedisTemplate<String, byte[]> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void cacheProfileImage(Long memberId, byte[] imageData) {
        String redisKey = PROFILE_IMG_PREFIX + memberId;
        redisTemplate.opsForValue().set(redisKey, imageData);
        redisTemplate.expire(redisKey, Duration.ofHours(1)); // 1시간 캐싱 예시
    }

    public byte[] getProfileImage(Long memberId) {
        return redisTemplate.opsForValue().get(PROFILE_IMG_PREFIX + memberId);
    }

    public void evictProfileImage(Long memberId) {
        redisTemplate.delete(PROFILE_IMG_PREFIX + memberId);
    }
}