package com.loopers.infrastructure.search;

import com.loopers.domain.brand.BrandService;
import com.loopers.domain.category.CategoryService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductCreatedEvent;
import com.loopers.domain.product.ProductDeletedEvent;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductSearchPort;
import com.loopers.domain.product.ProductUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(name = "spring.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class ProductIndexer {

    private final ProductSearchPort productSearchPort;
    private final ProductRepository productRepository;
    private final BrandService brandService;
    private final CategoryService categoryService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductCreated(ProductCreatedEvent event) {
        indexProduct(event.productId(), "생성");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductUpdated(ProductUpdatedEvent event) {
        indexProduct(event.productId(), "수정");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductDeleted(ProductDeletedEvent event) {
        try {
            productSearchPort.deleteProduct(event.productId());
            log.info("ES 상품 삭제 동기화 완료: productId={}", event.productId());
        } catch (Exception e) {
            log.error("ES 상품 삭제 동기화 실패: productId={}, error={}",
                    event.productId(), e.getMessage(), e);
        }
    }

    private void indexProduct(Long productId, String action) {
        try {
            Product product = productRepository.findById(productId).orElse(null);
            if (product == null) {
                log.warn("ES 상품 {} 동기화 스킵 — 상품 없음: productId={}", action, productId);
                return;
            }
            String brandName = resolveBrandName(product.getBrandId());
            String categoryName = resolveCategoryName(product.getCategoryId());
            productSearchPort.indexProduct(product, brandName, categoryName);
            log.info("ES 상품 {} 동기화 완료: productId={}", action, productId);
        } catch (Exception e) {
            log.error("ES 상품 {} 동기화 실패: productId={}, error={}",
                    action, productId, e.getMessage(), e);
        }
    }

    private String resolveBrandName(Long brandId) {
        if (brandId == null) return null;
        try {
            return brandService.getBrand(brandId).getName();
        } catch (Exception e) {
            log.warn("브랜드명 조회 실패: brandId={}", brandId);
            return null;
        }
    }

    private String resolveCategoryName(Long categoryId) {
        if (categoryId == null) return null;
        try {
            return categoryService.getById(categoryId).getName();
        } catch (Exception e) {
            log.warn("카테고리명 조회 실패: categoryId={}", categoryId);
            return null;
        }
    }
}
