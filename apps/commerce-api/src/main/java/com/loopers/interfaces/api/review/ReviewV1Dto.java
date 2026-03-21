package com.loopers.interfaces.api.review;

import com.loopers.application.review.ReviewFacade;
import com.loopers.application.review.ReviewInfo;
import com.loopers.interfaces.api.common.PageResponse;
import jakarta.validation.constraints.*;

import java.time.ZonedDateTime;

public class ReviewV1Dto {

    public record CreateReviewRequest(
            @NotNull(message = "주문 ID는 필수입니다.")
            Long orderId,

            @NotNull(message = "상품 ID는 필수입니다.")
            Long productId,

            @NotNull(message = "평점은 필수입니다.")
            @Min(value = 1, message = "평점은 1 이상이어야 합니다.")
            @Max(value = 5, message = "평점은 5 이하여야 합니다.")
            Integer rating,

            @NotBlank(message = "리뷰 내용은 필수입니다.")
            @Size(max = 500, message = "리뷰 내용은 500자 이하여야 합니다.")
            String content
    ) {}

    public record ReviewResponse(
            Long reviewId,
            Long orderId,
            Long productId,
            String userId,
            int rating,
            String content,
            ZonedDateTime createdAt
    ) {
        public static ReviewResponse from(ReviewInfo info) {
            return new ReviewResponse(
                    info.reviewId(),
                    info.orderId(),
                    info.productId(),
                    info.userId(),
                    info.rating(),
                    info.content(),
                    info.createdAt()
            );
        }
    }

    public record ProductReviewResponse(
            double averageRating,
            long totalCount,
            PageResponse<ReviewResponse> reviews
    ) {
        public static ProductReviewResponse from(ReviewFacade.ProductReviewInfo info) {
            PageResponse<ReviewResponse> reviewPage = info.reviews().map(ReviewResponse::from);
            return new ProductReviewResponse(
                    info.averageRating(),
                    info.totalCount(),
                    reviewPage
            );
        }
    }
}
