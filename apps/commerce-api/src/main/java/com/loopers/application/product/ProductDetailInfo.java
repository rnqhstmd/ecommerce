package com.loopers.application.product;

import com.loopers.domain.product.Product;

public record ProductDetailInfo(
        Long productId,
        String productName,
        Long price,
        Integer stock,
        Long brandId,
        Long likeCount,
        Boolean isLiked
) {
    public static ProductDetailInfo of(Product product, Long likeCount, Boolean isLiked) {
        return new ProductDetailInfo(
                product.getId(),
                product.getName(),
                product.getPriceValue(),
                product.getStockValue(),
                product.getBrandId(),
                likeCount,
                isLiked
        );
    }

    // 하위 호환용 오버로드 (createProduct 호출부 보호)
    public static ProductDetailInfo of(Product product, Long likeCount) {
        return of(product, likeCount, null);
    }
}
