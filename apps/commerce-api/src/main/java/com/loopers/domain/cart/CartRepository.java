package com.loopers.domain.cart;

import java.util.Map;

public interface CartRepository {
    /**
     * 장바구니에 상품을 추가한다. (HINCRBY 원자 연산으로 수량 합산)
     * @return 합산 후 현재 수량
     */
    long addItem(String userId, Long productId, int quantity);

    /**
     * 장바구니에서 특정 상품을 삭제한다.
     * @return 삭제 성공 여부 (해당 field가 존재했으면 true)
     */
    boolean removeItem(String userId, Long productId);

    /**
     * 장바구니 전체 항목을 조회한다.
     * @return productId -> quantity 맵
     */
    Map<Long, Integer> getCartItems(String userId);

    /**
     * 장바구니 전체를 삭제한다.
     */
    void deleteCart(String userId);

    /**
     * 특정 상품이 장바구니에 존재하는지 확인한다.
     */
    boolean existsItem(String userId, Long productId);

    /**
     * 장바구니 TTL을 7일로 갱신한다.
     */
    void refreshTtl(String userId);
}
