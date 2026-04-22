package com.loopers.application.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductSearchHit;
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

    public static ProductListInfo ofWithTotalHits(
            List<Product> products,
            Map<Long, Long> likeCountMap,
            Map<Long, Boolean> isLikedMap,
            Map<Long, ProductSearchHit> hitById,
            Pageable pageable,
            long totalHits
    ) {
        List<ProductContent> contents = products.stream()
                .map(product -> {
                    // hitById는 정상 경로에서 product.getId()를 항상 포함하므로 null은 이론적 엣지케이스.
                    // get() + null 체크로 ProductSearchHit.empty(...) eager 생성 비용을 제거한다.
                    ProductSearchHit hit = hitById.get(product.getId());
                    Map<String, List<String>> highlight = hit != null ? hit.highlights() : Map.of();
                    return ProductContent.of(
                            product,
                            likeCountMap.getOrDefault(product.getId(), 0L),
                            isLikedMap.isEmpty() ? null : isLikedMap.getOrDefault(product.getId(), false),
                            highlight
                    );
                })
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
            Boolean isLiked,
            Map<String, List<String>> highlight
    ) {
        public ProductContent {
            // 내층 리스트까지 deep copy하여 API 레이어까지 불변성을 보장한다.
            // 호출자가 원본 리스트를 수정해도 응답 데이터는 변하지 않는다.
            if (highlight == null || highlight.isEmpty()) {
                highlight = Map.of();
            } else {
                java.util.HashMap<String, List<String>> defensiveCopy = new java.util.HashMap<>();
                for (Map.Entry<String, List<String>> entry : highlight.entrySet()) {
                    List<String> value = entry.getValue();
                    if (value != null && !value.isEmpty()) {
                        defensiveCopy.put(entry.getKey(), List.copyOf(value));
                    }
                }
                highlight = Map.copyOf(defensiveCopy);
            }
        }

        public static ProductContent of(Product product, Long likeCount, Boolean isLiked,
                                        Map<String, List<String>> highlight) {
            return new ProductContent(
                    product.getId(),
                    product.getName(),
                    product.getPriceValue(),
                    product.getBrandId(),
                    likeCount,
                    isLiked,
                    highlight
            );
        }
    }
}
