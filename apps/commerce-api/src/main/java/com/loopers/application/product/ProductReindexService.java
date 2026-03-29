package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.category.Category;
import com.loopers.domain.category.CategoryService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductSearchPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

            var products = productPage.getContent();

            // 페이지 단위로 brandId/categoryId를 수집하여 한 번에 조회
            Set<Long> brandIds = products.stream()
                    .map(Product::getBrandId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Set<Long> categoryIds = products.stream()
                    .map(Product::getCategoryId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Map<Long, Brand> brandMap = brandIds.isEmpty()
                    ? Map.of()
                    : brandService.getBrandsByIds(brandIds);

            Map<Long, Category> categoryMap = categoryIds.isEmpty()
                    ? Map.of()
                    : categoryService.getCategoriesByIds(categoryIds);

            for (Product product : products) {
                try {
                    String brandName = resolveBrandName(product.getBrandId(), brandMap);
                    String categoryName = resolveCategoryName(product.getCategoryId(), categoryMap);
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

    private String resolveBrandName(Long brandId, Map<Long, Brand> brandMap) {
        if (brandId == null) return null;
        Brand brand = brandMap.get(brandId);
        if (brand == null) {
            log.warn("브랜드명 조회 실패: brandId={}", brandId);
            return null;
        }
        return brand.getName();
    }

    private String resolveCategoryName(Long categoryId, Map<Long, Category> categoryMap) {
        if (categoryId == null) return null;
        Category category = categoryMap.get(categoryId);
        if (category == null) {
            log.warn("카테고리명 조회 실패: categoryId={}", categoryId);
            return null;
        }
        return category.getName();
    }
}
