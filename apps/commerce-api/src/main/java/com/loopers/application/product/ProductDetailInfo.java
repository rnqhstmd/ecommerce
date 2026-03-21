package com.loopers.application.product;

import com.loopers.domain.product.Product;

public record ProductDetailInfo(
        Long productId,
        String productName,
        Long price,
        Integer stock,
        Long brandId,
        Long likeCount
) {
    public static ProductDetailInfo of(Product product, Long likeCount) {
        return new ProductDetailInfo(
                product.getId(),
                product.getName(),
                product.getPriceValue(),
                product.getStockValue(),
                product.getBrandId(),
                likeCount
        );
    }
}
