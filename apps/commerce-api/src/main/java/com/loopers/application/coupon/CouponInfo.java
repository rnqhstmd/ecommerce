package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponPolicy;
import com.loopers.domain.coupon.UserCoupon;

import java.time.ZonedDateTime;

public record CouponInfo(
        Long userCouponId,
        Long couponPolicyId,
        String couponName,
        String discountType,
        Long discountValue,
        ZonedDateTime issuedAt
) {
    public static CouponInfo from(UserCoupon userCoupon, CouponPolicy couponPolicy) {
        return new CouponInfo(
                userCoupon.getId(),
                couponPolicy.getId(),
                couponPolicy.getName(),
                couponPolicy.getDiscountType().name(),
                couponPolicy.getDiscountValue(),
                userCoupon.getIssuedAt()
        );
    }
}
