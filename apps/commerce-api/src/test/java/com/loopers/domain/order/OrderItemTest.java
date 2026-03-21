package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderItemTest {

    @Nested
    @DisplayName("주문 항목 생성 (OrderItem.create)")
    class CreateOrderItem {

        @DisplayName("상품 ID, 상품명, 단가, 수량으로 주문 항목을 생성할 수 있다.")
        @Test
        void createOrderItem() {
            // act
            OrderItem item = OrderItem.create(1L, "Test Product", 1000L, 3);

            // assert
            assertThat(item.getProductId()).isEqualTo(1L);
            assertThat(item.getProductName()).isEqualTo("Test Product");
            assertThat(item.getQuantity()).isEqualTo(3);
            assertThat(item.getOrder()).isNull();
        }

        @DisplayName("생성 시 단가가 OrderItemPrice로 스냅샷된다.")
        @Test
        void createOrderItem_snapshotsPrice() {
            // act
            OrderItem item = OrderItem.create(1L, "Test Product", 1000L, 3);

            // assert
            assertThat(item.getUnitPriceValue()).isEqualTo(1000L);
        }

        @DisplayName("단가가 null이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenPriceIsNull() {
            assertThatThrownBy(() -> OrderItemPrice.of(null))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("금액 계산")
    class CalculateAmount {

        @DisplayName("주문 항목의 총 금액 (단가 * 수량)을 계산할 수 있다.")
        @Test
        void calculateAmount() {
            // arrange
            OrderItem item = OrderItem.create(1L, "Test Product", 1000L, 3);

            // act
            OrderTotalAmount amount = item.calculateAmount();

            // assert
            assertThat(amount.getValue()).isEqualTo(3000L);
        }

        @DisplayName("수량이 1개일 때 총 금액을 계산할 수 있다.")
        @Test
        void calculateAmount_whenQuantityIsOne() {
            // arrange
            OrderItem item = OrderItem.create(1L, "Test Product", 1000L, 1);

            // act
            OrderTotalAmount amount = item.calculateAmount();

            // assert
            assertThat(amount.getValue()).isEqualTo(1000L);
        }
    }
}
