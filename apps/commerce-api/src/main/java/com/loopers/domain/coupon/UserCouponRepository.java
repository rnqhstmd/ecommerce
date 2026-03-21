package com.loopers.domain.coupon;

import java.util.Optional;

public interface UserCouponRepository {
    UserCoupon save(UserCoupon userCoupon);
    boolean existsByUserIdAndCouponPolicyId(String userId, Long couponPolicyId);
    Optional<UserCoupon> findById(Long id);
    Optional<UserCoupon> findByIdAndUserId(Long id, String userId);
}
