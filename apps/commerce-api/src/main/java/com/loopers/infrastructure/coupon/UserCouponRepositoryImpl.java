package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.coupon.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserCouponRepositoryImpl implements UserCouponRepository {

    private final UserCouponJpaRepository jpaRepository;

    @Override
    public UserCoupon save(UserCoupon userCoupon) {
        return jpaRepository.save(userCoupon);
    }

    @Override
    public boolean existsByUserIdAndCouponPolicyId(String userId, Long couponPolicyId) {
        return jpaRepository.existsByUserIdAndCouponPolicyId(userId, couponPolicyId);
    }

    @Override
    public Optional<UserCoupon> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<UserCoupon> findByIdAndUserId(Long id, String userId) {
        return jpaRepository.findByIdAndUserId(id, userId);
    }
}
