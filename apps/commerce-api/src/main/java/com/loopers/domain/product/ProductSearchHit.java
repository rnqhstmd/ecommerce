package com.loopers.domain.product;

import java.util.List;
import java.util.Map;

/**
 * ES 검색 결과 단일 hit를 표현하는 불변 값 객체.
 * productId와 필드별 하이라이트 조각을 함께 전달한다.
 *
 * <p>highlights 맵의 키는 하이라이트 대상 필드명(`name`, `brandName`, `categoryName`)이며,
 * 매칭된 필드만 키로 포함된다. 매칭이 없는 필드는 키 자체가 생략된다.
 * 값은 ES가 반환한 fragment 배열이다.</p>
 */
public record ProductSearchHit(Long productId, Map<String, List<String>> highlights) {

    public ProductSearchHit {
        highlights = highlights == null ? Map.of() : Map.copyOf(highlights);
    }

    public static ProductSearchHit of(Long productId, Map<String, List<String>> highlights) {
        return new ProductSearchHit(productId, highlights);
    }

    /**
     * 하이라이트가 없는 hit를 생성한다. 폴백(MySQL LIKE) 경로 등 하이라이트 미지원 상황에서 사용한다.
     */
    public static ProductSearchHit empty(Long productId) {
        return new ProductSearchHit(productId, Map.of());
    }
}
