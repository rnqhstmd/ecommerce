package com.loopers.application.product;

import com.loopers.domain.brand.BrandService;
import com.loopers.domain.category.CategoryService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductSearchPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductReindexService {

    private static final int BATCH_SIZE = 1000;

    private final ProductRepository productRepository;
    private final ProductSearchPort productSearchPort;
    private final BrandService brandService;
    private final CategoryService categoryService;

    public long reindexAll() {
        productSearchPort.deleteAllDocuments();
        log.info("재인덱싱 시작: ES 인덱스 전체 삭제 완료");

        long indexed = 0;
        int page = 0;

        while (true) {
            Page<Product> productPage = productRepository.findAllPaged(PageRequest.of(page, BATCH_SIZE));

            if (productPage.isEmpty()) break;

            for (Product product : productPage.getContent()) {
                try {
                    String brandName = resolveBrandName(product.getBrandId());
                    String categoryName = resolveCategoryName(product.getCategoryId());
                    productSearchPort.indexProduct(product, brandName, categoryName);
                    indexed++;
                } catch (Exception e) {
                    log.warn("재인덱싱 부분 실패: productId={}, error={}", product.getId(), e.getMessage());
                }
            }

            if (!productPage.hasNext()) break;
            page++;
        }

        log.info("재인덱싱 완료: indexed={}", indexed);
        return indexed;
    }

    private String resolveBrandName(Long brandId) {
        if (brandId == null) return null;
        try {
            return brandService.getBrand(brandId).getName();
        } catch (Exception e) {
            log.warn("브랜드명 조회 실패: brandId={}, error={}", brandId, e.getMessage());
            return null;
        }
    }

    private String resolveCategoryName(Long categoryId) {
        if (categoryId == null) return null;
        try {
            return categoryService.getById(categoryId).getName();
        } catch (Exception e) {
            log.warn("카테고리명 조회 실패: categoryId={}, error={}", categoryId, e.getMessage());
            return null;
        }
    }
}
