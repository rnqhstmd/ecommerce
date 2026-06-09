package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductPriceTest {

    @DisplayName("유효한 값으로 상품 가격을 생성할 수 있다.")
    @Test
    void createProductPrice() {
        // act
        ProductPrice price = ProductPrice.of(15000L);

        // assert
        assertThat(price.getValue()).isEqualTo(15000L);
    }

    @DisplayName("가격이 0이어도 생성할 수 있다.")
    @Test
    void createProductPrice_whenZero() {
        // act
        ProductPrice price = ProductPrice.of(0L);

        // assert
        assertThat(price.getValue()).isEqualTo(0L);
    }

    @DisplayName("가격이 null이면 BAD_REQUEST 예외가 발생한다.")
    @Test
    void throwsException_whenValueIsNull() {
        // act & assert
        assertThatThrownBy(() -> ProductPrice.of(null))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.BAD_REQUEST);
    }

    @DisplayName("가격이 음수이면 BAD_REQUEST 예외가 발생한다.")
    @Test
    void throwsException_whenValueIsNegative() {
        // act & assert
        assertThatThrownBy(() -> ProductPrice.of(-1L))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.BAD_REQUEST);
    }
}
