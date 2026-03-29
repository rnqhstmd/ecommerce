package com.loopers.application.product;

import com.loopers.domain.product.FacetBucket;
import com.loopers.domain.product.PriceRangeBucket;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductFacetResult;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductSearchCondition;
import com.loopers.domain.product.ProductSearchPort;
import com.loopers.domain.product.ProductSearchResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchService {

    private final ProductSearchPort productSearchPort;
    private final ProductRepository productRepository;

    @CircuitBreaker(name = "elasticsearchSearch", fallbackMethod = "searchFallback")
    public ProductSearchInfo search(ProductGetListCommand command) {
        ProductSearchResult result =
                productSearchPort.searchProducts(
                        command.keyword(),
                        command.brandId(),
                        command.minPrice(),
                        command.maxPrice(),
                        command.pageable().getPageNumber(),
                        command.pageable().getPageSize(),
                        command.pageable().getSort()
                );
        return new ProductSearchInfo(result.productIds(), result.totalHits());
    }

    @CircuitBreaker(name = "elasticsearchSearch", fallbackMethod = "autocompleteFallback")
    public List<String> autocomplete(String prefix, int size) {
        return productSearchPort.autocomplete(prefix, size);
    }

    @CircuitBreaker(name = "elasticsearchSearch", fallbackMethod = "facetsFallback")
    public ProductFacetInfo facets(String keyword, Long minPrice, Long maxPrice) {
        ProductFacetResult result =
                productSearchPort.facets(keyword, minPrice, maxPrice);
        return ProductFacetInfo.from(result);
    }

    // -- Fallback methods --

    private ProductSearchInfo searchFallback(ProductGetListCommand command, Throwable t) {
        log.warn("ES 검색 Circuit Breaker 폴백 -> MySQL LIKE: {}", t.getMessage());
        ProductSearchCondition condition = new ProductSearchCondition(
                command.brandId(), command.keyword(),
                command.minPrice(), command.maxPrice(), command.pageable()
        );
        Page<Product> page = productRepository.findProducts(condition);
        List<Long> ids = page.getContent().stream().map(Product::getId).toList();
        return new ProductSearchInfo(ids, page.getTotalElements());
    }

    private List<String> autocompleteFallback(String prefix, int size, Throwable t) {
        log.warn("ES 자동완성 Circuit Breaker 폴백 -> 빈 결과: {}", t.getMessage());
        return List.of();
    }

    private ProductFacetInfo facetsFallback(String keyword, Long minPrice, Long maxPrice, Throwable t) {
        log.warn("ES 집계 Circuit Breaker 폴백 -> 빈 결과: {}", t.getMessage());
        return ProductFacetInfo.empty();
    }
}
