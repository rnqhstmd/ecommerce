package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    @Cacheable(value = "product", key = "#id")
    public Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
    }

    public List<Product> getProductsByIds(Collection<Long> ids) {
        List<Product> products = productRepository.findAllByIds(ids);
        if (products.size() != ids.size()) {
            throw new CoreException(ErrorType.NOT_FOUND, "일부 상품을 찾을 수 없습니다.");
        }
        return products;
    }

    public List<Product> getProductsByIdsWithLock(List<Long> ids) {
        List<Product> products = productRepository.findAllByIdsWithLock(ids);
        if (products.size() != ids.size()) {
            throw new CoreException(ErrorType.NOT_FOUND, "일부 상품을 찾을 수 없습니다.");
        }
        return products;
    }

    public Page<Product> getProducts(ProductSearchCondition condition) {
        return productRepository.findProducts(condition.pageable(), condition.brandId());
    }

    @Transactional
    public Product createProduct(String name, Long price, Integer stock, Long brandId) {
        Product product = Product.create(name, price, stock, brandId);
        return productRepository.save(product);
    }

    @CacheEvict(value = "product", key = "#id")
    public void evictProductCache(Long id) {
        // 캐시 무효화만 수행
    }
}
