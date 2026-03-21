package com.loopers.domain.cart;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
class CartServiceIntegrationTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String USER_ID = "cart-integration-test-user";
    private static final Long PRODUCT_ID_1 = 100L;
    private static final Long PRODUCT_ID_2 = 200L;

    @BeforeEach
    void setUp() {
        // 테스트 전 장바구니 초기화
        redisTemplate.delete("cart:" + USER_ID);
    }

    @AfterEach
    void tearDown() {
        redisTemplate.delete("cart:" + USER_ID);
    }

    @DisplayName("장바구니에 상품을 추가하고 조회할 수 있다.")
    @Test
    void addItem_thenGetCartItems() {
        // act
        cartService.addItem(USER_ID, PRODUCT_ID_1, 2);

        // assert
        Map<Long, Integer> items = cartService.getCartItems(USER_ID);
        assertAll(
                () -> assertThat(items).hasSize(1),
                () -> assertThat(items.get(PRODUCT_ID_1)).isEqualTo(2)
        );
    }

    @DisplayName("동일 상품을 추가하면 수량이 합산된다.")
    @Test
    void addItem_accumulatesQuantity_forSameProduct() {
        // act
        cartService.addItem(USER_ID, PRODUCT_ID_1, 2);
        long accumulated = cartService.addItem(USER_ID, PRODUCT_ID_1, 3);

        // assert
        assertAll(
                () -> assertThat(accumulated).isEqualTo(5),
                () -> assertThat(cartService.getCartItems(USER_ID).get(PRODUCT_ID_1)).isEqualTo(5)
        );
    }

    @DisplayName("여러 상품을 추가하고 조회할 수 있다.")
    @Test
    void addMultipleItems_thenGetCartItems() {
        // act
        cartService.addItem(USER_ID, PRODUCT_ID_1, 1);
        cartService.addItem(USER_ID, PRODUCT_ID_2, 3);

        // assert
        Map<Long, Integer> items = cartService.getCartItems(USER_ID);
        assertAll(
                () -> assertThat(items).hasSize(2),
                () -> assertThat(items.get(PRODUCT_ID_1)).isEqualTo(1),
                () -> assertThat(items.get(PRODUCT_ID_2)).isEqualTo(3)
        );
    }

    @DisplayName("장바구니에서 특정 상품을 삭제할 수 있다.")
    @Test
    void removeItem_success() {
        // arrange
        cartService.addItem(USER_ID, PRODUCT_ID_1, 2);
        cartService.addItem(USER_ID, PRODUCT_ID_2, 1);

        // act
        cartService.removeItem(USER_ID, PRODUCT_ID_1);

        // assert
        Map<Long, Integer> items = cartService.getCartItems(USER_ID);
        assertAll(
                () -> assertThat(items).hasSize(1),
                () -> assertThat(items.containsKey(PRODUCT_ID_1)).isFalse(),
                () -> assertThat(items.get(PRODUCT_ID_2)).isEqualTo(1)
        );
    }

    @DisplayName("존재하지 않는 상품을 삭제하면 NOT_FOUND 예외가 발생한다.")
    @Test
    void removeItem_throwsNotFound_whenItemNotExists() {
        // act & assert
        assertThatThrownBy(() -> cartService.removeItem(USER_ID, 9999L))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.NOT_FOUND);
    }

    @DisplayName("장바구니 전체를 비울 수 있다.")
    @Test
    void clearCart_success() {
        // arrange
        cartService.addItem(USER_ID, PRODUCT_ID_1, 2);
        cartService.addItem(USER_ID, PRODUCT_ID_2, 3);

        // act
        cartService.clearCart(USER_ID);

        // assert
        Map<Long, Integer> items = cartService.getCartItems(USER_ID);
        assertThat(items).isEmpty();
    }

    @DisplayName("장바구니가 비어 있으면 빈 맵을 반환한다.")
    @Test
    void getCartItems_returnsEmpty_whenCartIsEmpty() {
        // act
        Map<Long, Integer> items = cartService.getCartItems(USER_ID);

        // assert
        assertThat(items).isEmpty();
    }

    @DisplayName("장바구니 추가 시 TTL이 설정된다.")
    @Test
    void addItem_setsTtl() {
        // act
        cartService.addItem(USER_ID, PRODUCT_ID_1, 1);

        // assert
        Long ttl = redisTemplate.getExpire("cart:" + USER_ID);
        assertThat(ttl).isNotNull();
        assertThat(ttl).isGreaterThan(0);
    }
}
