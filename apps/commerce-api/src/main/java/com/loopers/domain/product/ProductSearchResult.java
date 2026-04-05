package com.loopers.domain.product;

import java.util.List;

/**
 * ProductSearchPort의 반환 타입.
 * 검색 인프라 구현체가 생성하여 Application 레이어로 전달한다.
 *
 * <p>hits에는 결과 순서를 보존한 per-hit 레코드가 담긴다. 각 hit는 productId와 필드별 하이라이트 조각을 포함한다.
 * 총 건수는 totalHits로 노출된다.</p>
 */
public record ProductSearchResult(List<ProductSearchHit> hits, long totalHits) {

    /**
     * 정적 팩토리: hits와 총 건수로 결과를 생성한다.
     * record canonical constructor의 대칭 팩토리이며, 호출부 가독성과 컨벤션 일관성을 위해 제공한다.
     */
    public static ProductSearchResult of(List<ProductSearchHit> hits, long totalHits) {
        return new ProductSearchResult(hits, totalHits);
    }

    /**
     * 편의 접근자: hits에서 productId만 추출한다. 결과 순서는 보존된다.
     */
    public List<Long> productIds() {
        return hits.stream().map(ProductSearchHit::productId).toList();
    }

    public static ProductSearchResult empty() {
        return new ProductSearchResult(List.of(), 0L);
    }
}
