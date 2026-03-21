package com.loopers.infrastructure.cart;

import com.loopers.domain.cart.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RedisCartRepository implements CartRepository {

    private static final String CART_KEY_PREFIX = "cart:";
    private static final long TTL_DAYS = 7;

    private final RedisTemplate<String, String> redisTemplate;

    private String cartKey(String userId) {
        return CART_KEY_PREFIX + userId;
    }

    @Override
    public long addItem(String userId, Long productId, int quantity) {
        String key = cartKey(userId);
        Long result = redisTemplate.opsForHash().increment(key, productId.toString(), quantity);
        return result != null ? result : quantity;
    }

    @Override
    public boolean removeItem(String userId, Long productId) {
        String key = cartKey(userId);
        Long deleted = redisTemplate.opsForHash().delete(key, productId.toString());
        return deleted != null && deleted > 0;
    }

    @Override
    public Map<Long, Integer> getCartItems(String userId) {
        String key = cartKey(userId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        Map<Long, Integer> result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            Long productId = Long.parseLong(entry.getKey().toString());
            Integer quantity = Integer.parseInt(entry.getValue().toString());
            result.put(productId, quantity);
        }
        return result;
    }

    @Override
    public void deleteCart(String userId) {
        redisTemplate.delete(cartKey(userId));
    }

    @Override
    public boolean existsItem(String userId, Long productId) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForHash().hasKey(cartKey(userId), productId.toString())
        );
    }

    @Override
    public void refreshTtl(String userId) {
        redisTemplate.expire(cartKey(userId), TTL_DAYS, TimeUnit.DAYS);
    }
}
