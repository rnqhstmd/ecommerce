package com.loopers.application.product;

import org.springframework.data.domain.Pageable;

public record ProductGetListCommand(
        Long brandId,
        String userId,
        String keyword,
        Long minPrice,
        Long maxPrice,
        Pageable pageable
) {
}
