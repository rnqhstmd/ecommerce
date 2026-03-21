package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CouponPolicyRepository couponPolicyRepository;
    private final UserCouponRepository userCouponRepository;

    public CouponPolicy getCouponPolicy(Long id) {
        return couponPolicyRepository.findById(id)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰 정책을 찾을 수 없습니다."));
    }

    @Transactional
    public CouponPolicy saveCouponPolicy(CouponPolicy couponPolicy) {
        return couponPolicyRepository.save(couponPolicy);
    }

    @Transactional
    public UserCoupon issueUserCoupon(String userId, Long couponPolicyId) {
        if (userCouponRepository.existsByUserIdAndCouponPolicyId(userId, couponPolicyId)) {
            throw new CoreException(ErrorType.CONFLICT, "이미 발급받은 쿠폰입니다.");
        }
        UserCoupon userCoupon = UserCoupon.create(userId, couponPolicyId);
        return userCouponRepository.save(userCoupon);
    }

    public UserCoupon getUserCoupon(Long id) {
        return userCouponRepository.findById(id)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자 쿠폰을 찾을 수 없습니다."));
    }

    public UserCoupon getUserCouponByIdAndUserId(Long id, String userId) {
        return userCouponRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자 쿠폰을 찾을 수 없습니다."));
    }

    @Transactional
    public void markCouponUsed(UserCoupon userCoupon) {
        userCoupon.markUsed();
    }

    @Transactional
    public void restoreCoupon(Long userCouponId) {
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자 쿠폰을 찾을 수 없습니다."));
        userCoupon.markUnused();
    }

    public CouponPolicy getCouponPolicyWithLock(Long id) {
        return couponPolicyRepository.findByIdWithLock(id)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰 정책을 찾을 수 없습니다."));
    }
}
