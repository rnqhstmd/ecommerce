package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCouponJpaRepository extends JpaRepository<UserCoupon, Long> {
    boolean existsByUserIdAndCouponPolicyId(String userId, Long couponPolicyId);
    Optional<UserCoupon> findByIdAndUserId(Long id, String userId);
}
