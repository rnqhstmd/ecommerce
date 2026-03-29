package com.loopers.application.product;

import com.loopers.domain.product.FacetBucket;
import com.loopers.domain.product.PriceRangeBucket;
import com.loopers.domain.product.ProductFacetResult;

import java.util.List;

public record ProductFacetInfo(
        List<FacetBucket> brandFacets,
        List<FacetBucket> categoryFacets,
        List<PriceRangeBucket> priceRanges
) {
    public static ProductFacetInfo from(ProductFacetResult result) {
        return new ProductFacetInfo(
                result.brandFacets(),
                result.categoryFacets(),
                result.priceRanges()
        );
    }

    public static ProductFacetInfo empty() {
        return new ProductFacetInfo(List.of(), List.of(), List.of());
    }
}
