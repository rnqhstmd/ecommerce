package com.loopers.application.product;

import com.loopers.domain.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public record ProductListInfo(
        List<ProductContent> contents,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static ProductListInfo of(Page<Product> productPage,
                                     Map<Long, Long> likeCountMap,
                                     Map<Long, Boolean> isLikedMap) {
        List<ProductContent> contents = productPage.getContent().stream()
                .map(product -> ProductContent.of(
                        product,
                        likeCountMap.getOrDefault(product.getId(), 0L),
                        isLikedMap.isEmpty() ? null : isLikedMap.getOrDefault(product.getId(), false))
                )
                .toList();

        return new ProductListInfo(
                contents,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages()
        );
    }

    public static ProductListInfo ofWithTotalHits(
            List<Product> products,
            Map<Long, Long> likeCountMap,
            Map<Long, Boolean> isLikedMap,
            Pageable pageable,
            long totalHits
    ) {
        List<ProductContent> contents = products.stream()
                .map(product -> ProductContent.of(
                        product,
                        likeCountMap.getOrDefault(product.getId(), 0L),
                        isLikedMap.isEmpty() ? null : isLikedMap.getOrDefault(product.getId(), false))
                )
                .toList();

        int totalPages = pageable.getPageSize() > 0
                ? (int) Math.ceil((double) totalHits / pageable.getPageSize())
                : 0;
        return new ProductListInfo(
                contents,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                totalHits,
                totalPages
        );
    }

    public static ProductListInfo empty(Pageable pageable) {
        return new ProductListInfo(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0, 0);
    }

    public record ProductContent(
            Long id,
            String name,
            Long price,
            Long brandId,
            Long likeCount,
            Boolean isLiked
    ) {
        public static ProductContent of(Product product, Long likeCount, Boolean isLiked) {
            return new ProductContent(
                    product.getId(),
                    product.getName(),
                    product.getPriceValue(),
                    product.getBrandId(),
                    likeCount,
                    isLiked
            );
        }
    }
}
