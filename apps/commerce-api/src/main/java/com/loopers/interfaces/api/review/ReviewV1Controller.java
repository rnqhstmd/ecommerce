package com.loopers.interfaces.api.review;

import com.loopers.application.review.ReviewFacade;
import com.loopers.application.review.ReviewInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ReviewV1Controller {

    private final ReviewFacade reviewFacade;

    @PostMapping("/api/v1/reviews")
    public ApiResponse<ReviewV1Dto.ReviewResponse> createReview(
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @RequestBody @Valid ReviewV1Dto.CreateReviewRequest request
    ) {
        validateUserId(userId);
        ReviewInfo info = reviewFacade.createReview(
                userId, request.orderId(), request.productId(), request.rating(), request.content()
        );
        return ApiResponse.success(ReviewV1Dto.ReviewResponse.from(info));
    }

    @GetMapping("/api/v1/products/{productId}/reviews")
    public ApiResponse<ReviewV1Dto.ProductReviewResponse> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new CoreException(ErrorType.BAD_REQUEST, "page는 0 이상, size는 1 이상 100 이하여야 합니다.");
        }
        ReviewFacade.ProductReviewInfo info = reviewFacade.getProductReviews(productId, PageRequest.of(page, size));
        return ApiResponse.success(ReviewV1Dto.ProductReviewResponse.from(info));
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "X-USER-ID 헤더는 필수입니다.");
        }
    }
}
