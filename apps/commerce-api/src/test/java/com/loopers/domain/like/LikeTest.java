package com.loopers.domain.like;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LikeTest {

    @DisplayName("사용자 ID와 상품 ID로 좋아요를 생성할 수 있다.")
    @Test
    void createLike() {
        // arrange
        String userId = "testuser";
        Long productId = 1L;

        // act
        Like like = Like.create(userId, productId);

        // assert
        assertThat(like.getUserId()).isEqualTo(userId);
        assertThat(like.getProductId()).isEqualTo(productId);
    }
}
