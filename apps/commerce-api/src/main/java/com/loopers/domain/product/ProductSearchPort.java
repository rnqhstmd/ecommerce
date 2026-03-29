package com.loopers.domain.product;

import org.springframework.data.domain.Sort;

import java.util.List;

public interface ProductSearchPort {

    /**
     * 단일 상품을 ES 인덱스에 저장/갱신한다.
     */
    void indexProduct(Product product, String brandName, String categoryName);

    /**
     * ES 인덱스의 모든 문서를 삭제한다.
     */
    void deleteAllDocuments();

    /**
     * 단일 상품 문서를 ES 인덱스에서 삭제한다.
     */
    void deleteProduct(Long productId);

    /**
     * 멀티필드 검색 (name, brandName, categoryName).
     * keyword blank -> match_all.
     * brandId/minPrice/maxPrice는 filter context.
     * deletedAt null만 조회.
     *
     * @return 검색 결과 ID 목록 + 총 건수 (결과 순서 보존)
     */
    ProductSearchResult searchProducts(
            String keyword,
            Long brandId,
            Long minPrice,
            Long maxPrice,
            int page,
            int size,
            Sort sort
    );

    /**
     * 자동완성: 상품명 접두사 매칭.
     *
     * @return 중복 제거된 상품명 목록
     */
    List<String> autocomplete(String prefix, int size);

    /**
     * 집계: brandFacets + categoryFacets + priceRanges.
     */
    ProductFacetResult facets(
            String keyword,
            Long minPrice,
            Long maxPrice
    );
}
