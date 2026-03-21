package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductDetailInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public class ProductV1Dto {

    public record CreateRequest(
            @NotBlank(message = "상품명은 필수입니다.")
            String name,

            @NotNull(message = "가격은 필수입니다.")
            @Positive(message = "가격은 0보다 커야 합니다.")
            Long price,

            @NotNull(message = "재고는 필수입니다.")
            @PositiveOrZero(message = "재고는 0 이상이어야 합니다.")
            Integer stock,

            @NotNull(message = "브랜드 ID는 필수입니다.")
            Long brandId
    ) {}

    public record ProductResponse(
            Long productId,
            String productName,
            Long price,
            Integer stock,
            Long brandId,
            Long likeCount
    ) {
        public static ProductResponse from(ProductDetailInfo info) {
            return new ProductResponse(
                    info.productId(),
                    info.productName(),
                    info.price(),
                    info.stock(),
                    info.brandId(),
                    info.likeCount()
            );
        }
    }
}
