package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponPolicyTest {

    private static final ZonedDateTime NOW = ZonedDateTime.now();
    private static final ZonedDateTime VALID_FROM = NOW.minusDays(1);
    private static final ZonedDateTime VALID_TO = NOW.plusDays(30);

    @Nested
    @DisplayName("쿠폰 정책 생성 (CouponPolicy.create)")
    class CreateCouponPolicy {

        @DisplayName("정률 할인 쿠폰을 생성할 수 있다.")
        @Test
        void createRateCoupon() {
            // act
            CouponPolicy policy = CouponPolicy.create("10% 할인", DiscountType.RATE, 10L, VALID_FROM, VALID_TO, 100);

            // assert
            assertThat(policy.getName()).isEqualTo("10% 할인");
            assertThat(policy.getDiscountType()).isEqualTo(DiscountType.RATE);
            assertThat(policy.getDiscountValue()).isEqualTo(10L);
            assertThat(policy.getTotalQuantity()).isEqualTo(100);
            assertThat(policy.getIssuedQuantity()).isZero();
        }

        @DisplayName("정액 할인 쿠폰을 생성할 수 있다.")
        @Test
        void createAmountCoupon() {
            // act
            CouponPolicy policy = CouponPolicy.create("1000원 할인", DiscountType.AMOUNT, 1000L, VALID_FROM, VALID_TO, 50);

            // assert
            assertThat(policy.getName()).isEqualTo("1000원 할인");
            assertThat(policy.getDiscountType()).isEqualTo(DiscountType.AMOUNT);
            assertThat(policy.getDiscountValue()).isEqualTo(1000L);
        }
    }

    @Nested
    @DisplayName("할인 계산 (calculateDiscount)")
    class CalculateDiscount {

        @DisplayName("정률 할인: 10% 할인 시 10000원 -> 1000원 할인")
        @Test
        void rateDiscount() {
            // arrange
            CouponPolicy policy = CouponPolicy.create("10% 할인", DiscountType.RATE, 10L, VALID_FROM, VALID_TO, 100);

            // act
            long discount = policy.calculateDiscount(10000L);

            // assert
            assertThat(discount).isEqualTo(1000L);
        }

        @DisplayName("정률 할인: 할인액이 총액을 초과하지 않는다.")
        @Test
        void rateDiscount_doesNotExceedTotal() {
            // arrange
            CouponPolicy policy = CouponPolicy.create("100% 할인", DiscountType.RATE, 100L, VALID_FROM, VALID_TO, 100);

            // act
            long discount = policy.calculateDiscount(5000L);

            // assert
            assertThat(discount).isEqualTo(5000L);
        }

        @DisplayName("정액 할인: 1000원 할인 쿠폰 적용")
        @Test
        void amountDiscount() {
            // arrange
            CouponPolicy policy = CouponPolicy.create("1000원 할인", DiscountType.AMOUNT, 1000L, VALID_FROM, VALID_TO, 100);

            // act
            long discount = policy.calculateDiscount(10000L);

            // assert
            assertThat(discount).isEqualTo(1000L);
        }

        @DisplayName("정액 할인: 할인액이 총액을 초과하면 총액만큼만 할인된다.")
        @Test
        void amountDiscount_doesNotExceedTotal() {
            // arrange
            CouponPolicy policy = CouponPolicy.create("5000원 할인", DiscountType.AMOUNT, 5000L, VALID_FROM, VALID_TO, 100);

            // act
            long discount = policy.calculateDiscount(3000L);

            // assert
            assertThat(discount).isEqualTo(3000L);
        }
    }

    @Nested
    @DisplayName("유효기간 검증 (isValid)")
    class ValidPeriod {

        @DisplayName("현재 시간이 유효기간 내이면 true를 반환한다.")
        @Test
        void isValid_withinPeriod() {
            // arrange
            CouponPolicy policy = CouponPolicy.create("쿠폰", DiscountType.RATE, 10L,
                    NOW.minusDays(1), NOW.plusDays(1), 100);

            // act & assert
            assertThat(policy.isValid()).isTrue();
        }

        @DisplayName("유효기간이 지났으면 false를 반환한다.")
        @Test
        void isValid_expired() {
            // arrange
            CouponPolicy policy = CouponPolicy.create("쿠폰", DiscountType.RATE, 10L,
                    NOW.minusDays(10), NOW.minusDays(1), 100);

            // act & assert
            assertThat(policy.isValid()).isFalse();
        }

        @DisplayName("유효기간 시작 전이면 false를 반환한다.")
        @Test
        void isValid_notStarted() {
            // arrange
            CouponPolicy policy = CouponPolicy.create("쿠폰", DiscountType.RATE, 10L,
                    NOW.plusDays(1), NOW.plusDays(10), 100);

            // act & assert
            assertThat(policy.isValid()).isFalse();
        }
    }

    @Nested
    @DisplayName("발급 수량 검증")
    class QuantityValidation {

        @DisplayName("발급 수량이 0 이하이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenTotalQuantityIsZero() {
            // act & assert
            assertThatThrownBy(() -> CouponPolicy.create("쿠폰", DiscountType.RATE, 10L, VALID_FROM, VALID_TO, 0))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("발급 수량이 음수이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenTotalQuantityIsNegative() {
            // act & assert
            assertThatThrownBy(() -> CouponPolicy.create("쿠폰", DiscountType.RATE, 10L, VALID_FROM, VALID_TO, -1))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("increaseIssuedQuantity 호출 시 발급 수량이 증가한다.")
        @Test
        void increaseIssuedQuantity() {
            // arrange
            CouponPolicy policy = CouponPolicy.create("쿠폰", DiscountType.RATE, 10L, VALID_FROM, VALID_TO, 100);

            // act
            policy.increaseIssuedQuantity();

            // assert
            assertThat(policy.getIssuedQuantity()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("유효기간 필드 검증")
    class PeriodFieldValidation {

        @DisplayName("시작일이 종료일보다 이후이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenValidFromAfterValidTo() {
            // act & assert
            assertThatThrownBy(() -> CouponPolicy.create("쿠폰", DiscountType.RATE, 10L,
                    NOW.plusDays(10), NOW.plusDays(1), 100))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("유효기간이 null이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenValidFromIsNull() {
            // act & assert
            assertThatThrownBy(() -> CouponPolicy.create("쿠폰", DiscountType.RATE, 10L, null, VALID_TO, 100))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
