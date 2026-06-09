package com.loopers.domain.point;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PointBalanceTest {

    @Nested
    @DisplayName("포인트 잔액 생성 (PointBalance.of / zero)")
    class Create {

        @DisplayName("유효한 값으로 포인트 잔액을 생성할 수 있다.")
        @Test
        void createPointBalance() {
            // act
            PointBalance balance = PointBalance.of(1000L);

            // assert
            assertThat(balance.getValue()).isEqualTo(1000L);
        }

        @DisplayName("zero()로 잔액 0인 포인트 잔액을 생성할 수 있다.")
        @Test
        void createZeroBalance() {
            // act
            PointBalance balance = PointBalance.zero();

            // assert
            assertThat(balance.getValue()).isEqualTo(0L);
        }

        @DisplayName("값이 null이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenValueIsNull() {
            // act & assert
            assertThatThrownBy(() -> PointBalance.of(null))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("값이 음수이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenValueIsNegative() {
            // act & assert
            assertThatThrownBy(() -> PointBalance.of(-1L))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("포인트 충전 (charge)")
    class Charge {

        @DisplayName("충전하면 충전액만큼 증가한 새 잔액을 반환한다.")
        @Test
        void chargeIncreasesBalance() {
            // arrange
            PointBalance balance = PointBalance.of(1000L);

            // act
            PointBalance charged = balance.charge(500L);

            // assert
            assertThat(charged.getValue()).isEqualTo(1500L);
            assertThat(balance.getValue()).isEqualTo(1000L);
        }

        @DisplayName("충전액이 0 이하이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenChargeAmountIsZeroOrNegative() {
            // arrange
            PointBalance balance = PointBalance.of(1000L);

            // act & assert
            assertThatThrownBy(() -> balance.charge(0L))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);

            assertThatThrownBy(() -> balance.charge(-100L))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("충전액이 null이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenChargeAmountIsNull() {
            // arrange
            PointBalance balance = PointBalance.of(1000L);

            // act & assert
            assertThatThrownBy(() -> balance.charge(null))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("충전 시 Long 최대값을 초과하면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenChargeExceedsMaxValue() {
            // arrange
            PointBalance balance = PointBalance.of(Long.MAX_VALUE - 10L);

            // act & assert
            assertThatThrownBy(() -> balance.charge(100L))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("포인트 사용 (use)")
    class Use {

        @DisplayName("사용하면 사용액만큼 감소한 새 잔액을 반환한다.")
        @Test
        void useDecreasesBalance() {
            // arrange
            PointBalance balance = PointBalance.of(1000L);

            // act
            PointBalance used = balance.use(400L);

            // assert
            assertThat(used.getValue()).isEqualTo(600L);
            assertThat(balance.getValue()).isEqualTo(1000L);
        }

        @DisplayName("사용액이 0 이하이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenUseAmountIsZeroOrNegative() {
            // arrange
            PointBalance balance = PointBalance.of(1000L);

            // act & assert
            assertThatThrownBy(() -> balance.use(0L))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);

            assertThatThrownBy(() -> balance.use(-100L))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("보유 잔액보다 많이 사용하면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenUseAmountExceedsBalance() {
            // arrange
            PointBalance balance = PointBalance.of(1000L);

            // act & assert
            assertThatThrownBy(() -> balance.use(1500L))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
