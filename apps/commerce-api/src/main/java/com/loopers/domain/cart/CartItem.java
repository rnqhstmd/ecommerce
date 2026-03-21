package com.loopers.domain.cart;

public record CartItem(
        Long productId,
        int quantity
) {
}
