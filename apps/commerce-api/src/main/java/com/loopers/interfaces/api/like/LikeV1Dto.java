package com.loopers.interfaces.api.like;

import jakarta.validation.constraints.NotNull;

public class LikeV1Dto {

    public record LikeRequest(
            @NotNull(message = "상품 ID는 필수입니다.")
            Long productId
    ) {}

    public record LikeResponse(
            String userId,
            Long productId
    ) {
        public static LikeResponse of(String userId, Long productId) {
            return new LikeResponse(userId, productId);
        }
    }

    public record LikeItemResponse(Long productId) {
        public static LikeItemResponse of(Long productId) {
            return new LikeItemResponse(productId);
        }
    }
}
