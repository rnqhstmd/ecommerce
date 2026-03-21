package com.loopers.domain.cart;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CartService cartService;

    private static final String USER_ID = "test-user";
    private static final Long PRODUCT_ID = 1L;

    @Nested
    @DisplayName("장바구니 추가 (addItem)")
    class AddItem {

        @DisplayName("유효한 수량으로 장바구니에 상품을 추가할 수 있다.")
        @Test
        void addItem_success() {
            // arrange
            when(cartRepository.addItem(USER_ID, PRODUCT_ID, 3)).thenReturn(3L);

            // act
            long result = cartService.addItem(USER_ID, PRODUCT_ID, 3);

            // assert
            assertThat(result).isEqualTo(3L);
            verify(cartRepository).addItem(USER_ID, PRODUCT_ID, 3);
            verify(cartRepository).refreshTtl(USER_ID);
        }

        @DisplayName("동일 상품을 다시 추가하면 합산된 수량이 반환된다.")
        @Test
        void addItem_accumulatesQuantity() {
            // arrange
            when(cartRepository.addItem(USER_ID, PRODUCT_ID, 2)).thenReturn(5L);

            // act
            long result = cartService.addItem(USER_ID, PRODUCT_ID, 2);

            // assert
            assertThat(result).isEqualTo(5L);
        }

        @DisplayName("수량이 0 이하이면 BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(ints = {0, -1, -100})
        void addItem_throwsException_whenQuantityIsInvalid(int invalidQuantity) {
            // act & assert
            assertThatThrownBy(() -> cartService.addItem(USER_ID, PRODUCT_ID, invalidQuantity))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);

            verify(cartRepository, never()).addItem(anyString(), anyLong(), anyInt());
        }
    }

    @Nested
    @DisplayName("장바구니 삭제 (removeItem)")
    class RemoveItem {

        @DisplayName("장바구니에 존재하는 상품을 삭제할 수 있다.")
        @Test
        void removeItem_success() {
            // arrange
            when(cartRepository.removeItem(USER_ID, PRODUCT_ID)).thenReturn(true);

            // act
            cartService.removeItem(USER_ID, PRODUCT_ID);

            // assert
            verify(cartRepository).removeItem(USER_ID, PRODUCT_ID);
        }

        @DisplayName("장바구니에 없는 상품을 삭제하면 NOT_FOUND 예외가 발생한다.")
        @Test
        void removeItem_throwsException_whenItemNotFound() {
            // arrange
            when(cartRepository.removeItem(USER_ID, PRODUCT_ID)).thenReturn(false);

            // act & assert
            assertThatThrownBy(() -> cartService.removeItem(USER_ID, PRODUCT_ID))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("장바구니 조회 (getCartItems)")
    class GetCartItems {

        @DisplayName("장바구니 항목을 조회할 수 있다.")
        @Test
        void getCartItems_success() {
            // arrange
            Map<Long, Integer> items = Map.of(1L, 2, 2L, 3);
            when(cartRepository.getCartItems(USER_ID)).thenReturn(items);

            // act
            Map<Long, Integer> result = cartService.getCartItems(USER_ID);

            // assert
            assertThat(result).hasSize(2);
            assertThat(result.get(1L)).isEqualTo(2);
            assertThat(result.get(2L)).isEqualTo(3);
        }

        @DisplayName("장바구니가 비어 있으면 빈 맵을 반환한다.")
        @Test
        void getCartItems_returnsEmptyMap_whenCartIsEmpty() {
            // arrange
            when(cartRepository.getCartItems(USER_ID)).thenReturn(Map.of());

            // act
            Map<Long, Integer> result = cartService.getCartItems(USER_ID);

            // assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("장바구니 전체 비우기 (clearCart)")
    class ClearCart {

        @DisplayName("장바구니를 전체 비울 수 있다.")
        @Test
        void clearCart_success() {
            // act
            cartService.clearCart(USER_ID);

            // assert
            verify(cartRepository).deleteCart(USER_ID);
        }
    }
}
