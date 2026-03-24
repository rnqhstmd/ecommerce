package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponFacade;
import com.loopers.application.coupon.CouponInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.auth.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponV1Controller implements CouponV1ApiSpec {

    private final CouponFacade couponFacade;

    @PostMapping("/{id}/issue")
    @Override
    public ApiResponse<CouponV1Dto.CouponIssueResponse> issueCoupon(
            @PathVariable Long id
    ) {
        String userId = SecurityContextHelper.getCurrentUserId();
        CouponInfo info = couponFacade.issueCoupon(id, userId);
        return ApiResponse.success(CouponV1Dto.CouponIssueResponse.from(info));
    }
}
