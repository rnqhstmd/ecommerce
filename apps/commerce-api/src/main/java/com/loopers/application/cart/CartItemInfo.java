package com.loopers.application.cart;

import com.loopers.domain.product.Product;

public record CartItemInfo(
        Long productId,
        String productName,
        Long price,
        int quantity,
        String stockStatus
) {
    public static CartItemInfo of(Product product, int quantity) {
        String status = product.getStockValue() > 0 ? "IN_STOCK" : "OUT_OF_STOCK";
        return new CartItemInfo(
                product.getId(),
                product.getName(),
                product.getPriceValue(),
                quantity,
                status
        );
    }
}
