package com.loopers.application.product;

import java.util.List;

public record ProductSearchInfo(
        List<Long> productIds,
        long totalHits
) {}
