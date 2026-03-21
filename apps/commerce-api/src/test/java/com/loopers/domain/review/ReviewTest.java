package com.loopers.domain.review;

import com.loopers.domain.order.Order;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewTest {

    private Order dummyOrder;
    private Long dummyProductId;
    private String dummyUserId;

    @BeforeEach
    void setUp() {
        dummyOrder = Order.create("testuser");
        dummyOrder.addOrderItem(1L, "Product A", 1000L, 1);
        dummyOrder.completePayment();
        dummyProductId = 1L;
        dummyUserId = "testuser";
    }

    @Nested
    @DisplayName("리뷰 생성 (Review.create)")
    class CreateReview {

        @DisplayName("유효한 입력으로 리뷰를 생성할 수 있다.")
        @Test
        void createReview() {
            // act
            Review review = Review.create(dummyOrder, dummyProductId, dummyUserId, 5, "좋은 상품입니다.");

            // assert
            assertThat(review.getProductId()).isEqualTo(dummyProductId);
            assertThat(review.getUserId()).isEqualTo(dummyUserId);
            assertThat(review.getRating()).isEqualTo(5);
            assertThat(review.getContent()).isEqualTo("좋은 상품입니다.");
        }

        @DisplayName("평점 1~5 사이 값으로 리뷰를 생성할 수 있다.")
        @ParameterizedTest
        @ValueSource(ints = {1, 2, 3, 4, 5})
        void createReview_withValidRating(int rating) {
            // act
            Review review = Review.create(dummyOrder, dummyProductId, dummyUserId, rating, "리뷰 내용");

            // assert
            assertThat(review.getRating()).isEqualTo(rating);
        }
    }

    @Nested
    @DisplayName("평점 범위 검증 (rating 1~5)")
    class RatingValidation {

        @DisplayName("평점이 1 미만이면 BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(ints = {0, -1, -10})
        void throwsException_whenRatingBelowMin(int invalidRating) {
            // act & assert
            assertThatThrownBy(() -> Review.create(dummyOrder, dummyProductId, dummyUserId, invalidRating, "내용"))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("평점이 5 초과이면 BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(ints = {6, 10, 100})
        void throwsException_whenRatingAboveMax(int invalidRating) {
            // act & assert
            assertThatThrownBy(() -> Review.create(dummyOrder, dummyProductId, dummyUserId, invalidRating, "내용"))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("리뷰 내용 검증 (content)")
    class ContentValidation {

        @DisplayName("내용이 null이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenContentIsNull() {
            // act & assert
            assertThatThrownBy(() -> Review.create(dummyOrder, dummyProductId, dummyUserId, 5, null))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("내용이 빈 문자열이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenContentIsBlank() {
            // act & assert
            assertThatThrownBy(() -> Review.create(dummyOrder, dummyProductId, dummyUserId, 5, "   "))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("내용이 500자 초과이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenContentExceeds500() {
            // arrange
            String longContent = "가".repeat(501);

            // act & assert
            assertThatThrownBy(() -> Review.create(dummyOrder, dummyProductId, dummyUserId, 5, longContent))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("내용이 정확히 500자이면 리뷰를 생성할 수 있다.")
        @Test
        void createReview_withExactly500Chars() {
            // arrange
            String content = "가".repeat(500);

            // act
            Review review = Review.create(dummyOrder, dummyProductId, dummyUserId, 5, content);

            // assert
            assertThat(review.getContent()).hasSize(500);
        }
    }
}
