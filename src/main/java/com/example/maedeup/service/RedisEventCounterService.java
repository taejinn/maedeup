package com.example.maedeup.service;

import com.example.maedeup.repository.ParticipationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisEventCounterService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ParticipationRepository participationRepository;

    private static final String PARTICIPANT_COUNT_KEY = "event:participant_count:";
    private static final String PARTICIPANT_SET_KEY = "event:participants:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private static final String INCREMENT_SCRIPT = 
        "local setKey = KEYS[1] " +
        "local countKey = KEYS[2] " +
        "local userId = ARGV[1] " +
        "local maxParticipants = tonumber(ARGV[2]) " +
        "local ttl = tonumber(ARGV[3]) " +
        "local addResult = redis.call('SADD', setKey, userId) " +
        "if addResult == 0 then " +
        "  return {0, redis.call('GET', countKey) or 0} " +
        "end " +
        "local currentCount = redis.call('GET', countKey) or 0 " +
        "if tonumber(currentCount) >= maxParticipants then " +
        "  redis.call('SREM', setKey, userId) " +
        "  return {-1, currentCount} " +
        "end " +
        "local newCount = redis.call('INCR', countKey) " +
        "redis.call('EXPIRE', setKey, ttl) " +
        "redis.call('EXPIRE', countKey, ttl) " +
        "return {1, newCount}";

    private static final String DECREMENT_SCRIPT = 
        "local setKey = KEYS[1] " +
        "local countKey = KEYS[2] " +
        "local userId = ARGV[1] " +
        "local removeResult = redis.call('SREM', setKey, userId) " +
        "if removeResult == 0 then " +
        "  return redis.call('GET', countKey) or 0 " +
        "end " +
        "local newCount = redis.call('DECR', countKey) " +
        "if newCount < 0 then " +
        "  redis.call('SET', countKey, 0) " +
        "  return 0 " +
        "end " +
        "return newCount";

    public ParticipationResult tryParticipate(Long eventId, Long userId, Integer maxParticipants) {
        try {
            DefaultRedisScript<List> script = new DefaultRedisScript<>();
            script.setScriptText(INCREMENT_SCRIPT);
            script.setResultType(List.class);

            List<String> keys = Arrays.asList(
                getParticipantSetKey(eventId),
                getParticipantCountKey(eventId)
            );
            
            List<String> args = Arrays.asList(
                userId.toString(),
                maxParticipants != null ? maxParticipants.toString() : "999999",
                String.valueOf(DEFAULT_TTL.getSeconds())
            );

            List<Long> result = redisTemplate.execute(script, keys, args.toArray());
            
            if (result != null && result.size() == 2) {
                int status = result.get(0).intValue();
                long count = result.get(1);
                
                return switch (status) {
                    case 1 -> new ParticipationResult(true, count, "SUCCESS");
                    case 0 -> new ParticipationResult(false, count, "ALREADY_PARTICIPATED");
                    case -1 -> new ParticipationResult(false, count, "CAPACITY_EXCEEDED");
                    default -> new ParticipationResult(false, count, "UNKNOWN_ERROR");
                };
            }
        } catch (Exception e) {
            log.warn("Redis operation failed, falling back to DB: {}", e.getMessage());
        }
        
        return fallbackToDatabase(eventId, userId, maxParticipants);
    }

    public Long decrementParticipantCount(Long eventId, Long userId) {
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(DECREMENT_SCRIPT);
            script.setResultType(Long.class);

            List<String> keys = Arrays.asList(
                getParticipantSetKey(eventId),
                getParticipantCountKey(eventId)
            );

            Long result = redisTemplate.execute(script, keys, userId.toString());
            return result != null ? result : 0L;
        } catch (Exception e) {
            log.warn("Redis decrement failed, falling back to DB count: {}", e.getMessage());
            return getCurrentParticipantCountFromDB(eventId);
        }
    }

    public Long getCurrentParticipantCount(Long eventId) {
        try {
            String countKey = getParticipantCountKey(eventId);
            Object count = redisTemplate.opsForValue().get(countKey);
            return count != null ? Long.valueOf(count.toString()) : getCurrentParticipantCountFromDB(eventId);
        } catch (Exception e) {
            log.warn("Redis get failed, falling back to DB: {}", e.getMessage());
            return getCurrentParticipantCountFromDB(eventId);
        }
    }

    public Boolean isUserParticipated(Long eventId, Long userId) {
        try {
            String setKey = getParticipantSetKey(eventId);
            Boolean result = redisTemplate.opsForSet().isMember(setKey, userId.toString());
            if (result != null) {
                return result;
            }
        } catch (Exception e) {
            log.warn("Redis membership check failed, falling back to DB: {}", e.getMessage());
        }
        
        return participationRepository.existsByUserIdAndEventId(userId, eventId);
    }

    public void initializeParticipantCount(Long eventId, Long initialCount) {
        try {
            String countKey = getParticipantCountKey(eventId);
            redisTemplate.opsForValue().set(countKey, initialCount, DEFAULT_TTL);
        } catch (Exception e) {
            log.warn("Redis initialization failed: {}", e.getMessage());
        }
    }

    public void clearEventData(Long eventId) {
        try {
            String countKey = getParticipantCountKey(eventId);
            String setKey = getParticipantSetKey(eventId);
            
            redisTemplate.delete(countKey);
            redisTemplate.delete(setKey);
        } catch (Exception e) {
            log.warn("Redis clear failed: {}", e.getMessage());
        }
    }

    public Set<Object> getAllParticipants(Long eventId) {
        try {
            String setKey = getParticipantSetKey(eventId);
            return redisTemplate.opsForSet().members(setKey);
        } catch (Exception e) {
            log.warn("Redis get all participants failed: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    private ParticipationResult fallbackToDatabase(Long eventId, Long userId, Integer maxParticipants) {
        if (participationRepository.existsByUserIdAndEventId(userId, eventId)) {
            return new ParticipationResult(false, getCurrentParticipantCountFromDB(eventId), "ALREADY_PARTICIPATED");
        }

        long currentCount = getCurrentParticipantCountFromDB(eventId);
        if (maxParticipants != null && currentCount >= maxParticipants) {
            return new ParticipationResult(false, currentCount, "CAPACITY_EXCEEDED");
        }

        return new ParticipationResult(true, currentCount, "SUCCESS");
    }

    private Long getCurrentParticipantCountFromDB(Long eventId) {
        return participationRepository.countByEventId(eventId);
    }

    public static class ParticipationResult {
        public final boolean canParticipate;
        public final long currentCount;
        public final String reason;

        public ParticipationResult(boolean canParticipate, long currentCount, String reason) {
            this.canParticipate = canParticipate;
            this.currentCount = currentCount;
            this.reason = reason;
        }
    }

    private String getParticipantCountKey(Long eventId) {
        return PARTICIPANT_COUNT_KEY + eventId;
    }

    private String getParticipantSetKey(Long eventId) {
        return PARTICIPANT_SET_KEY + eventId;
    }
} 