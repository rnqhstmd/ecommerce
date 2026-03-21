package com.loopers.domain.product;

import org.springframework.data.domain.Pageable;

public record ProductSearchCondition(
        Long brandId,
        String keyword,
        Long minPrice,
        Long maxPrice,
        Pageable pageable
) {
}
