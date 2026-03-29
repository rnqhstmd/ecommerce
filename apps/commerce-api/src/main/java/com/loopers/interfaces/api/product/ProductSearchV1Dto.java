package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductFacetInfo;
import java.util.List;

public class ProductSearchV1Dto {

    public record AutocompleteResponse(List<String> suggestions) {}

    public record FacetResponse(
            List<FacetBucket> brandFacets,
            List<FacetBucket> categoryFacets,
            List<PriceRangeBucket> priceRanges
    ) {
        public static FacetResponse from(ProductFacetInfo info) {
            return new FacetResponse(
                    info.brandFacets().stream()
                            .map(b -> new FacetBucket(b.name(), b.count())).toList(),
                    info.categoryFacets().stream()
                            .map(b -> new FacetBucket(b.name(), b.count())).toList(),
                    info.priceRanges().stream()
                            .map(b -> new PriceRangeBucket(b.range(), b.count())).toList()
            );
        }
    }

    public record FacetBucket(String name, long count) {}
    public record PriceRangeBucket(String range, long count) {}
}
