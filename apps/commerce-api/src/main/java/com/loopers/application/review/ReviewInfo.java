package com.loopers.application.review;

import com.loopers.domain.review.Review;

import java.time.ZonedDateTime;

public record ReviewInfo(
        Long reviewId,
        Long orderId,
        Long productId,
        String userId,
        int rating,
        String content,
        ZonedDateTime createdAt
) {
    public static ReviewInfo from(Review review) {
        return new ReviewInfo(
                review.getId(),
                review.getOrderId(),
                review.getProductId(),
                review.getUserId(),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt()
        );
    }
}
