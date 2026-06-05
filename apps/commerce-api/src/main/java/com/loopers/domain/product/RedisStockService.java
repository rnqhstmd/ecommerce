package com.loopers.domain.product;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis 원자 재고 선차감 서비스 (개선안).
 * 비관적 락(SELECT ... FOR UPDATE) 대신 Redis DECRBY로 재고 게이트를 통과시켜
 * 핫상품 동시 주문 시의 DB 락 경합을 제거한다.
 */
@Service
@RequiredArgsConstructor
public class RedisStockService {

    private final StringRedisTemplate redis;

    private static String key(long productId) {
        return "stock:" + productId;
    }

    /** MySQL 재고를 Redis로 적재(워밍업). */
    public void warmUp(long productId, long stock) {
        redis.opsForValue().set(key(productId), String.valueOf(stock));
    }

    /** 원자적 선차감: 성공 true, 재고부족 false(롤백). */
    public boolean tryDeduct(long productId, long qty) {
        Long remain = redis.opsForValue().decrement(key(productId), qty);
        if (remain == null) {
            return false;
        }
        if (remain < 0) {
            redis.opsForValue().increment(key(productId), qty); // 롤백
            return false;
        }
        return true;
    }

    /** 차감 복구(주문 실패/취소 시). */
    public void restore(long productId, long qty) {
        redis.opsForValue().increment(key(productId), qty);
    }

    public long current(long productId) {
        String v = redis.opsForValue().get(key(productId));
        return v == null ? 0 : Long.parseLong(v);
    }
}
