package com.example.maedeup.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisEventCounterService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PARTICIPANT_COUNT_KEY = "event:participant_count:";
    private static final String PARTICIPANT_SET_KEY = "event:participants:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    public Long incrementParticipantCount(Long eventId, Long userId) {
        String countKey = getParticipantCountKey(eventId);
        String setKey = getParticipantSetKey(eventId);

        Long addResult = redisTemplate.opsForSet().add(setKey, userId.toString());
        if (addResult == null || addResult == 0) {
            return getCurrentParticipantCount(eventId);
        }

        Long count = redisTemplate.opsForValue().increment(countKey);
        
        redisTemplate.expire(countKey, DEFAULT_TTL);
        redisTemplate.expire(setKey, DEFAULT_TTL);

        return count;
    }

    public Long getCurrentParticipantCount(Long eventId) {
        String countKey = getParticipantCountKey(eventId);
        Object count = redisTemplate.opsForValue().get(countKey);
        return count != null ? Long.valueOf(count.toString()) : 0L;
    }

    public Boolean isUserParticipated(Long eventId, Long userId) {
        String setKey = getParticipantSetKey(eventId);
        return redisTemplate.opsForSet().isMember(setKey, userId.toString());
    }

    public Long decrementParticipantCount(Long eventId, Long userId) {
        String countKey = getParticipantCountKey(eventId);
        String setKey = getParticipantSetKey(eventId);

        Long removeResult = redisTemplate.opsForSet().remove(setKey, userId.toString());
        if (removeResult == null || removeResult == 0) {
            return getCurrentParticipantCount(eventId);
        }

        Long count = redisTemplate.opsForValue().decrement(countKey);
        if (count < 0) {
            redisTemplate.opsForValue().set(countKey, 0);
            count = 0L;
        }

        return count;
    }

    public void initializeParticipantCount(Long eventId, Long initialCount) {
        String countKey = getParticipantCountKey(eventId);
        redisTemplate.opsForValue().set(countKey, initialCount, DEFAULT_TTL);
    }

    public void clearEventData(Long eventId) {
        String countKey = getParticipantCountKey(eventId);
        String setKey = getParticipantSetKey(eventId);
        
        redisTemplate.delete(countKey);
        redisTemplate.delete(setKey);
    }

    public Set<Object> getAllParticipants(Long eventId) {
        String setKey = getParticipantSetKey(eventId);
        return redisTemplate.opsForSet().members(setKey);
    }

    public Boolean canParticipate(Long eventId, Integer maxParticipants, Long userId) {
        if (Boolean.TRUE.equals(isUserParticipated(eventId, userId))) {
            return false;
        }

        Long currentCount = getCurrentParticipantCount(eventId);
        return currentCount < maxParticipants;
    }

    private String getParticipantCountKey(Long eventId) {
        return PARTICIPANT_COUNT_KEY + eventId;
    }

    private String getParticipantSetKey(Long eventId) {
        return PARTICIPANT_SET_KEY + eventId;
    }
} 