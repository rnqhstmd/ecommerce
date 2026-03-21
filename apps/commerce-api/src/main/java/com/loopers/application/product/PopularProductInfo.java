package com.loopers.application.product;

import com.loopers.domain.product.Product;

public record PopularProductInfo(
        Long productId,
        String productName,
        Long price,
        Long likeCount
) {
    public static PopularProductInfo of(Product product, Long likeCount) {
        return new PopularProductInfo(
                product.getId(),
                product.getName(),
                product.getPriceValue(),
                likeCount
        );
    }
}
