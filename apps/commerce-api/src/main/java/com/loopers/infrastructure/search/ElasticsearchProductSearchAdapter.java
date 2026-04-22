package com.loopers.infrastructure.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.loopers.domain.product.FacetBucket;
import com.loopers.domain.product.PriceRangeBucket;
import com.loopers.domain.product.ProductFacetResult;
import com.loopers.domain.product.ProductSearchHit;
import com.loopers.domain.product.ProductSearchResult;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductSearchPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "spring.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchProductSearchAdapter implements ProductSearchPort {

    private static final String INDEX_NAME = "products";

    private final ElasticsearchClient esClient;
    private final ProductSearchRepository productSearchRepository;

    @Override
    public void indexProduct(Product product, String brandName, String categoryName) {
        ProductDocument document = ProductDocument.from(product, brandName, categoryName);
        productSearchRepository.save(document);
    }

    @Override
    public void deleteAllDocuments() {
        productSearchRepository.deleteAll();
    }

    @Override
    public void deleteProduct(Long productId) {
        productSearchRepository.deleteById(productId);
    }

    @Override
    public ProductSearchResult searchProducts(
            String keyword,
            Long brandId,
            Long minPrice,
            Long maxPrice,
            int page,
            int size,
            org.springframework.data.domain.Sort sort
    ) {
        try {
            List<Query> filters = buildFilters(brandId, minPrice, maxPrice);

            boolean hasKeyword = keyword != null && !keyword.isBlank();
            Query mainQuery;
            if (!hasKeyword) {
                mainQuery = Query.of(q -> q.matchAll(m -> m));
            } else {
                mainQuery = Query.of(q -> q
                        .multiMatch(mm -> mm
                                .query(keyword)
                                .fields("name", "brandName", "categoryName")
                                .type(TextQueryType.BestFields)
                                .tieBreaker(0.3)
                                .fuzziness("AUTO")
                        )
                );
            }

            List<SortOptions> sortOptions = buildSortOptions(sort);

            Query finalQuery = mainQuery;
            SearchResponse<ProductDocument> response = esClient.search(s -> {
                        s.index(INDEX_NAME)
                            .query(q -> q
                                    .bool(b -> {
                                        b.must(finalQuery);
                                        filters.forEach(b::filter);
                                        return b;
                                    })
                            )
                            .from(page * size)
                            .size(size);
                        if (hasKeyword) {
                            s.highlight(buildHighlight());
                        }
                        if (!sortOptions.isEmpty()) {
                            s.sort(sortOptions);
                        }
                        return s;
                    },
                    ProductDocument.class
            );

            List<ProductSearchHit> hits = response.hits().hits().stream()
                    .filter(hit -> hit.source() != null)
                    .map(this::toSearchHit)
                    .toList();

            long totalHits = response.hits().total() != null
                    ? response.hits().total().value() : 0L;

            return ProductSearchResult.of(hits, totalHits);
        } catch (IOException e) {
            log.error("ES 검색 실패", e);
            throw new UncheckedIOException(e);
        }
    }

    /**
     * name/brandName/categoryName 세 필드에 대한 하이라이트 설정을 빌드한다.
     * 매칭 단어는 {@code <em>...</em>} 태그로 감싸진다.
     */
    private Highlight buildHighlight() {
        return Highlight.of(h -> h
                .preTags("<em>")
                .postTags("</em>")
                .fields("name", HighlightField.of(hf -> hf))
                .fields("brandName", HighlightField.of(hf -> hf))
                .fields("categoryName", HighlightField.of(hf -> hf))
        );
    }

    /**
     * ES Hit에서 productId와 필드별 하이라이트 조각을 추출해 ProductSearchHit로 변환한다.
     * 빈 리스트 값은 필터링하여 매칭 없는 필드 키가 남지 않도록 한다.
     *
     * <p><strong>호출 계약</strong>: 호출자는 {@code hit.source() != null}을 보장해야 한다.
     * 이 메서드는 source null 체크를 수행하지 않으며, 현재 유일한 진입점은
     * {@link #searchProducts}의 스트림에서 {@code .filter(hit -> hit.source() != null)}을
     * 거친 후에만 호출된다.</p>
     */
    private ProductSearchHit toSearchHit(Hit<ProductDocument> hit) {
        Long productId = hit.source().getId();
        Map<String, List<String>> rawHighlight = hit.highlight();
        if (rawHighlight == null || rawHighlight.isEmpty()) {
            return ProductSearchHit.empty(productId);
        }
        // ProductSearchHit의 compact constructor가 내층 List까지 deep copy 및 빈 값 필터링을 수행하므로
        // 여기서는 순서 보장 목적 없이 단순 HashMap으로 전달한다. JSON 응답에서 키 순서는 규정되지 않는다.
        Map<String, List<String>> filtered = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : rawHighlight.entrySet()) {
            List<String> value = entry.getValue();
            if (value != null && !value.isEmpty()) {
                filtered.put(entry.getKey(), value);
            }
        }
        return ProductSearchHit.of(productId, filtered);
    }

    @Override
    public List<String> autocomplete(String prefix, int size) {
        try {
            List<Query> filters = List.of(deletedAtFilter());
            Query prefixQuery = Query.of(q -> q
                    .matchPhrasePrefix(mp -> mp
                            .field("name.autocomplete")
                            .query(prefix)
                    )
            );

            SearchResponse<ProductDocument> response = esClient.search(s -> s
                            .index(INDEX_NAME)
                            .query(q -> q
                                    .bool(b -> {
                                        b.must(prefixQuery);
                                        filters.forEach(b::filter);
                                        return b;
                                    })
                            )
                            .size(size)
                            .source(sc -> sc.filter(sf -> sf.includes("name"))),
                    ProductDocument.class
            );

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(doc -> doc != null)
                    .map(ProductDocument::getName)
                    .distinct()
                    .toList();
        } catch (IOException e) {
            log.error("ES 자동완성 실패", e);
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public ProductFacetResult facets(
            String keyword,
            Long minPrice,
            Long maxPrice
    ) {
        try {
            List<Query> filters = buildFilters(null, minPrice, maxPrice);

            Query mainQuery;
            if (keyword == null || keyword.isBlank()) {
                mainQuery = Query.of(q -> q.matchAll(m -> m));
            } else {
                mainQuery = Query.of(q -> q
                        .multiMatch(mm -> mm
                                .query(keyword)
                                .fields("name", "brandName", "categoryName")
                                .type(TextQueryType.BestFields)
                                .tieBreaker(0.3)
                        )
                );
            }

            Query finalQuery = mainQuery;
            SearchResponse<ProductDocument> response = esClient.search(s -> s
                            .index(INDEX_NAME)
                            .size(0)
                            .query(q -> q
                                    .bool(b -> {
                                        b.must(finalQuery);
                                        filters.forEach(b::filter);
                                        return b;
                                    })
                            )
                            .aggregations("brand_facets", a -> a
                                    .terms(t -> t.field("brandName.keyword").size(50))
                            )
                            .aggregations("category_facets", a -> a
                                    .terms(t -> t.field("categoryName.keyword").size(50))
                            )
                            .aggregations("price_ranges", a -> a
                                    .range(r -> r
                                            .field("price")
                                            .ranges(rng -> rng.key("~10,000").to(10000.0))
                                            .ranges(rng -> rng.key("10,000~50,000").from(10000.0).to(50000.0))
                                            .ranges(rng -> rng.key("50,000~100,000").from(50000.0).to(100000.0))
                                            .ranges(rng -> rng.key("100,000~").from(100000.0))
                                    )
                            ),
                    ProductDocument.class
            );

            // brand facets 파싱
            List<FacetBucket> brandFacets = new ArrayList<>();
            StringTermsAggregate brandAgg = response.aggregations().get("brand_facets").sterms();
            for (StringTermsBucket bucket : brandAgg.buckets().array()) {
                brandFacets.add(new FacetBucket(bucket.key().stringValue(), bucket.docCount()));
            }

            // category facets 파싱
            List<FacetBucket> categoryFacets = new ArrayList<>();
            StringTermsAggregate categoryAgg = response.aggregations().get("category_facets").sterms();
            for (StringTermsBucket bucket : categoryAgg.buckets().array()) {
                categoryFacets.add(new FacetBucket(bucket.key().stringValue(), bucket.docCount()));
            }

            // price ranges 파싱
            List<PriceRangeBucket> priceRanges = new ArrayList<>();
            RangeAggregate priceAgg = response.aggregations().get("price_ranges").range();
            for (RangeBucket bucket : priceAgg.buckets().array()) {
                priceRanges.add(new PriceRangeBucket(bucket.key(), bucket.docCount()));
            }

            return new ProductFacetResult(brandFacets, categoryFacets, priceRanges);
        } catch (IOException e) {
            log.error("ES 집계 실패", e);
            throw new UncheckedIOException(e);
        }
    }

    private List<SortOptions> buildSortOptions(org.springframework.data.domain.Sort sort) {
        List<SortOptions> sortOptions = new ArrayList<>();
        if (sort == null || sort.isUnsorted()) {
            return sortOptions;
        }
        for (org.springframework.data.domain.Sort.Order order : sort) {
            String field = mapSortField(order.getProperty());
            SortOrder direction = order.isAscending() ? SortOrder.Asc : SortOrder.Desc;
            sortOptions.add(SortOptions.of(so -> so.field(f -> f.field(field).order(direction))));
        }
        return sortOptions;
    }

    private String mapSortField(String property) {
        return switch (property) {
            case "price.value" -> "price";
            case "createdAt" -> "createdAt";
            case "likeCount" -> "likeCount";
            default -> property;
        };
    }

    private Query deletedAtFilter() {
        return Query.of(f -> f
                .bool(fb -> fb
                        .mustNot(mn -> mn.exists(e -> e.field("deletedAt")))
                )
        );
    }

    private List<Query> buildFilters(Long brandId, Long minPrice, Long maxPrice) {
        List<Query> filters = new ArrayList<>();

        filters.add(deletedAtFilter());

        if (brandId != null) {
            filters.add(Query.of(f -> f.term(t -> t.field("brandId").value(brandId))));
        }
        if (minPrice != null || maxPrice != null) {
            filters.add(Query.of(f -> f
                    .range(r -> r
                            .number(nr -> {
                                nr.field("price");
                                if (minPrice != null) nr.gte(minPrice.doubleValue());
                                if (maxPrice != null) nr.lte(maxPrice.doubleValue());
                                return nr;
                            })
                    )
            ));
        }

        return filters;
    }
}
