package com.loopers.application.product;

import java.util.List;

public record ProductFacetResult(
        List<FacetBucket> brandFacets,
        List<FacetBucket> categoryFacets,
        List<PriceRangeBucket> priceRanges
) {}
