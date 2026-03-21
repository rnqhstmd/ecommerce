package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponPolicy;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CouponFacade {

    private static final String COUPON_STOCK_KEY_PREFIX = "coupon:stock:";

    private final CouponService couponService;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public CouponInfo issueCoupon(Long couponPolicyId, String userId) {
        CouponPolicy policy = couponService.getCouponPolicy(couponPolicyId);

        if (!policy.isValid()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 유효기간이 아닙니다.");
        }

        String stockKey = COUPON_STOCK_KEY_PREFIX + couponPolicyId;

        // Redis에 키가 없으면 초기화
        Boolean hasKey = redisTemplate.hasKey(stockKey);
        if (Boolean.FALSE.equals(hasKey)) {
            int remaining = policy.getTotalQuantity() - policy.getIssuedQuantity();
            redisTemplate.opsForValue().set(stockKey, String.valueOf(remaining));
        }

        // Redis DECR로 원자적 수량 제어
        Long remaining = redisTemplate.opsForValue().decrement(stockKey);
        if (remaining == null || remaining < 0) {
            // 복구
            redisTemplate.opsForValue().increment(stockKey);
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰이 모두 소진되었습니다.");
        }

        try {
            UserCoupon userCoupon = couponService.issueUserCoupon(userId, couponPolicyId);
            policy.increaseIssuedQuantity();
            couponService.saveCouponPolicy(policy);
            return CouponInfo.from(userCoupon, policy);
        } catch (CoreException e) {
            // 중복 발급 등 실패 시 Redis 복구
            redisTemplate.opsForValue().increment(stockKey);
            throw e;
        }
    }
}
