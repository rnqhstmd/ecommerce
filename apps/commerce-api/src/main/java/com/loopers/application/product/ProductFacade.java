package com.loopers.application.product;

import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductCreatedEvent;
import com.loopers.domain.product.ProductSearchHit;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.ProductUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ProductFacade — Application layer orchestrator.
 * Controllers delegate to this facade; it coordinates domain/application services.
 */

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProductFacade {

    private final ProductService productService;
    private final LikeService likeService;
    private final PopularProductService popularProductService;
    private final ProductSearchService productSearchService;
    private final ProductReindexService productReindexService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ProductDetailInfo createProduct(String name, Long price, Integer stock, Long brandId) {
        Product product = productService.createProduct(name, price, stock, brandId);
        eventPublisher.publishEvent(new ProductCreatedEvent(product.getId()));
        return ProductDetailInfo.of(product, 0L);
    }

    public ProductDetailInfo getProductDetail(Long productId, String userId) {
        Product product = productService.getProduct(productId);
        Long likeCount = likeService.getLikeCount(product.getId());
        Boolean isLiked = likeService.getIsLiked(userId, product.getId());
        return ProductDetailInfo.of(product, likeCount, isLiked);
    }

    // 하위 호환 오버로드
    public ProductDetailInfo getProductDetail(Long productId) {
        return getProductDetail(productId, null);
    }

    @Transactional
    public ProductDetailInfo updateProduct(Long productId, String name, Long price) {
        Product product = productService.getProductWithLock(productId);
        String oldName = product.getName();
        Long oldPrice = product.getPriceValue();

        product.updateName(name);
        product.updatePrice(price);

        productService.evictProductCache(productId);

        log.info("상품 수정: id={}, name: {} -> {}, price: {} -> {}",
                productId, oldName, product.getName(), oldPrice, product.getPriceValue());

        eventPublisher.publishEvent(new ProductUpdatedEvent(productId));

        Long likeCount = likeService.getLikeCount(productId);
        return ProductDetailInfo.of(product, likeCount);
    }

    @Transactional
    public ProductDetailInfo increaseStock(Long productId, Integer quantity) {
        Product product = productService.getProductWithLock(productId);
        product.increaseStock(quantity);
        productService.evictProductCache(productId);

        Long likeCount = likeService.getLikeCount(productId);
        return ProductDetailInfo.of(product, likeCount);
    }

    public List<PopularProductInfo> getPopularProducts(int limit) {
        return popularProductService.getPopularProducts(limit);
    }

    public List<String> autocomplete(String keyword, int size) {
        return productSearchService.autocomplete(keyword, size);
    }

    public ProductFacetInfo facets(String keyword, Long minPrice, Long maxPrice) {
        return productSearchService.facets(keyword, minPrice, maxPrice);
    }

    public long reindexAll() {
        return productReindexService.reindexAll();
    }

    public ProductListInfo getProducts(ProductGetListCommand command) {
        // Phase 1: ES에서 Hit 목록 + 총 건수 조회
        ProductSearchInfo searchInfo = productSearchService.search(command);

        if (searchInfo.hits().isEmpty()) {
            return ProductListInfo.empty(command.pageable());
        }

        // hit 인덱싱: productId -> ProductSearchHit (highlight lookup)
        // ES 결과에서 동일 productId 중복은 정상 상황이 아니지만, 안정성을 위해 첫 번째 값을 유지하는 merge function 지정.
        Map<Long, ProductSearchHit> hitById = searchInfo.hits().stream()
                .collect(Collectors.toMap(
                        ProductSearchHit::productId,
                        Function.identity(),
                        (existing, replacement) -> existing));

        // Phase 2: MySQL에서 상세 조회 (ES 결과 순서 보존)
        List<Product> products = productService.findProductsByIds(searchInfo.productIds());

        // ES 결과 순서대로 정렬
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        List<Product> orderedProducts = searchInfo.productIds().stream()
                .map(productMap::get)
                .filter(Objects::nonNull)
                .toList();

        List<Long> productIds = orderedProducts.stream().map(Product::getId).toList();
        Map<Long, Long> likeCountMap = likeService.getLikeCountsByProductIds(productIds);
        Map<Long, Boolean> isLikedMap = likeService.getIsLikedMap(command.userId(), productIds);

        return ProductListInfo.ofWithTotalHits(
                orderedProducts, likeCountMap, isLikedMap,
                hitById, command.pageable(), searchInfo.totalHits()
        );
    }
}
