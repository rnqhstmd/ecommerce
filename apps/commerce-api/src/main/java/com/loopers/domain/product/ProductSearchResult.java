package com.loopers.domain.product;

import java.util.List;

public record ProductSearchResult(List<Long> productIds, long totalHits) {}
