package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponInfo;

import java.time.ZonedDateTime;

public class CouponV1Dto {

    public record CouponIssueResponse(
            Long userCouponId,
            Long couponPolicyId,
            String couponName,
            String discountType,
            Long discountValue,
            ZonedDateTime issuedAt
    ) {
        public static CouponIssueResponse from(CouponInfo info) {
            return new CouponIssueResponse(
                    info.userCouponId(),
                    info.couponPolicyId(),
                    info.couponName(),
                    info.discountType(),
                    info.discountValue(),
                    info.issuedAt()
            );
        }
    }
}
