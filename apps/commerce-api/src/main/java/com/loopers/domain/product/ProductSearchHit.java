package com.loopers.domain.product;

import java.util.HashMap;
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
        highlights = deepCopyHighlights(highlights);
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

    /**
     * 하이라이트 Map을 내층 List까지 deep copy하여 불변 Map으로 반환한다.
     * 빈 리스트를 가진 키는 제거된다. API 응답 레이어까지 공유 사용을 위해 static으로 노출한다.
     */
    public static Map<String, List<String>> deepCopyHighlights(Map<String, List<String>> highlights) {
        if (highlights == null || highlights.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> defensiveCopy = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : highlights.entrySet()) {
            List<String> value = entry.getValue();
            if (value != null && !value.isEmpty()) {
                defensiveCopy.put(entry.getKey(), List.copyOf(value));
            }
        }
        return Map.copyOf(defensiveCopy);
    }
}
