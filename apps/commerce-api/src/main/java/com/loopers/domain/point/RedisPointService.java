package com.loopers.domain.point;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis 원자 포인트 잔액 서비스 (개선안).
 * 포인트 비관적 락(SELECT ... FOR UPDATE) + UPDATE + 이력 INSERT 대신
 * Redis DECRBY로 잔액을 원자적으로 차감해 주문 핫패스의 DB 락 경합을 제거한다.
 */
@Service
@RequiredArgsConstructor
public class RedisPointService {

    private final StringRedisTemplate redis;

    private static String key(String userId) {
        return "point:balance:" + userId;
    }

    public void charge(String userId, long amount) {
        redis.opsForValue().increment(key(userId), amount);
    }

    /** 원자적 차감: 성공 true, 잔액부족 false(롤백). */
    public boolean tryUse(String userId, long amount) {
        Long remain = redis.opsForValue().decrement(key(userId), amount);
        if (remain == null) {
            return false;
        }
        if (remain < 0) {
            redis.opsForValue().increment(key(userId), amount);
            return false;
        }
        return true;
    }

    public void restore(String userId, long amount) {
        redis.opsForValue().increment(key(userId), amount);
    }

    public long current(String userId) {
        String v = redis.opsForValue().get(key(userId));
        return v == null ? 0 : Long.parseLong(v);
    }
}
