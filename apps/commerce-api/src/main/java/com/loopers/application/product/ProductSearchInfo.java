package com.loopers.application.product;

import com.loopers.domain.product.ProductSearchHit;
import com.loopers.domain.product.ProductSearchResult;

import java.util.List;

/**
 * Application 레이어의 검색 결과 DTO.
 * ProductSearchResult를 Application 경계에 맞춰 감싼다.
 *
 * <p>hits는 per-hit productId + 하이라이트 조각을 포함한다.
 * 폴백(MySQL LIKE) 경로에서는 highlight 미지원으로 모든 hit의 highlights가 빈 맵이다.</p>
 */
public record ProductSearchInfo(
        List<ProductSearchHit> hits,
        long totalHits
) {

    /**
     * 편의 접근자: hits에서 productId만 추출한다. 결과 순서는 보존된다.
     */
    public List<Long> productIds() {
        return hits.stream().map(ProductSearchHit::productId).toList();
    }

    /**
     * 정상 경로: ES 검색 결과를 그대로 매핑한다.
     */
    public static ProductSearchInfo from(ProductSearchResult result) {
        return new ProductSearchInfo(result.hits(), result.totalHits());
    }

    /**
     * 폴백 전용 팩토리. 하이라이트를 지원하지 않는 MySQL LIKE 경로에서 사용하며,
     * 모든 hit의 highlights는 빈 맵으로 채워진다.
     */
    public static ProductSearchInfo fromIdsWithoutHighlight(List<Long> ids, long totalHits) {
        List<ProductSearchHit> hits = ids.stream()
                .map(ProductSearchHit::empty)
                .toList();
        return new ProductSearchInfo(hits, totalHits);
    }
}
