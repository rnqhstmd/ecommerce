package com.loopers.support.lock;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
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
public class DistributedLockService {

    private final RedissonClient redissonClient;

    public void executeWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Runnable action) {
        executeWithLock(key, waitTime, leaseTime, unit, () -> {
            action.run();
            return null;
        });
    }

    public <T> T executeWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(key);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitTime, leaseTime, unit);
            if (!acquired) {
                log.warn("분산 락 획득 실패: key={}", key);
                throw new CoreException(ErrorType.CONFLICT, "다른 요청이 처리 중입니다. 잠시 후 다시 시도해주세요.");
            }
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CoreException(ErrorType.INTERNAL_ERROR, "락 획득 중 인터럽트가 발생했습니다.");
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
