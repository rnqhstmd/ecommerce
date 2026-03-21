package com.loopers.interfaces.api.product;

import com.loopers.application.product.PopularProductInfo;
import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductListInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

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
            Long likeCount,
            Boolean isLiked
    ) {
        public static ProductResponse from(ProductDetailInfo info) {
            return new ProductResponse(
                    info.productId(),
                    info.productName(),
                    info.price(),
                    info.stock(),
                    info.brandId(),
                    info.likeCount(),
                    info.isLiked()
            );
        }
    }

    public record ProductListResponse(
            List<ProductContentResponse> contents,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        public static ProductListResponse from(ProductListInfo info) {
            List<ProductContentResponse> contents = info.contents().stream()
                    .map(ProductContentResponse::from).toList();
            return new ProductListResponse(contents, info.page(), info.size(),
                    info.totalElements(), info.totalPages());
        }
    }

    public record UpdateRequest(
            String name,
            Long price
    ) {}

    public record StockRequest(
            @NotNull(message = "수량은 필수입니다.")
            @Positive(message = "수량은 1 이상이어야 합니다.")
            Integer quantity
    ) {}

    public record StockResponse(
            Long productId,
            Integer stock
    ) {}

    public record PopularProductsResponse(
            List<PopularProductItem> products
    ) {
        public static PopularProductsResponse from(List<PopularProductInfo> infos) {
            List<PopularProductItem> items = infos.stream()
                    .map(PopularProductItem::from)
                    .toList();
            return new PopularProductsResponse(items);
        }
    }

    public record PopularProductItem(
            Long productId,
            String productName,
            Long price,
            Long likeCount
    ) {
        public static PopularProductItem from(PopularProductInfo info) {
            return new PopularProductItem(
                    info.productId(),
                    info.productName(),
                    info.price(),
                    info.likeCount()
            );
        }
    }

    public record ProductContentResponse(
            Long id,
            String name,
            Long price,
            Long brandId,
            Long likeCount,
            Boolean isLiked
    ) {
        public static ProductContentResponse from(ProductListInfo.ProductContent content) {
            return new ProductContentResponse(content.id(), content.name(), content.price(),
                    content.brandId(), content.likeCount(), content.isLiked());
        }
    }
}
