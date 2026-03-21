package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponFacade;
import com.loopers.application.coupon.CouponInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponV1Controller {

    private final CouponFacade couponFacade;

    @PostMapping("/{id}/issue")
    public ApiResponse<CouponV1Dto.CouponIssueResponse> issueCoupon(
            @PathVariable Long id,
            @RequestHeader(value = "X-USER-ID", required = false) String userId
    ) {
        validateUserId(userId);
        CouponInfo info = couponFacade.issueCoupon(id, userId);
        return ApiResponse.success(CouponV1Dto.CouponIssueResponse.from(info));
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "X-USER-ID 헤더는 필수입니다.");
        }
    }
}
