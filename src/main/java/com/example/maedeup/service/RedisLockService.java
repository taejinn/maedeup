package com.example.maedeup.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisLockService {

    private final RedissonClient redissonClient;

    private static final String LOCK_PREFIX = "lock:";
    private static final long WAIT_TIME = 10L;
    private static final long LEASE_TIME = 5L;
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        return executeWithLock(lockKey, WAIT_TIME, LEASE_TIME, TimeUnit.SECONDS, supplier);
    }


    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Supplier<T> supplier) {
        String fullLockKey = LOCK_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(fullLockKey);

        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (!acquired) {
                throw new RuntimeException("락 획득에 실패했습니다. 잠시 후 다시 시도해주세요.");
            }

            return supplier.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("락 획득 중 인터럽트가 발생했습니다.");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


    public void executeWithLock(String lockKey, Runnable runnable) {
        executeWithLock(lockKey, () -> {
            runnable.run();
            return null;
        });
    }


    public String getEventLockKey(Long eventId) {
        return "event:" + eventId;
    }


    public String getParticipationLockKey(Long eventId, Long userId) {
        return "participation:" + eventId + ":" + userId;
    }
} 