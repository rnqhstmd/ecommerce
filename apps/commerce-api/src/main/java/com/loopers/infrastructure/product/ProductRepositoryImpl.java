package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductSearchCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository jpaRepository;
    private final ProductQueryRepository queryRepository;

    @Override
    public Product save(Product product) {
        return jpaRepository.save(product);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Product> findAllByIds(Collection<Long> ids) {
        return jpaRepository.findAllByIds(ids);
    }

    @Override
    public List<Product> findAllByIdsWithLock(List<Long> ids) {
        return jpaRepository.findAllByIdsWithLock(ids);
    }

    @Override
    public Optional<Product> findByIdWithLock(Long id) {
        return jpaRepository.findByIdWithLock(id);
    }

    @Override
    public Page<Product> findProducts(ProductSearchCondition condition) {
        return queryRepository.findProducts(condition);
    }

    @Override
    public void incrementLikeCount(Long productId) {
        jpaRepository.incrementLikeCount(productId);
    }

    @Override
    public void decrementLikeCount(Long productId) {
        jpaRepository.decrementLikeCount(productId);
    }

    @Override
    public List<Product> findTopByLikeCountDesc(int limit) {
        return jpaRepository.findTopByLikeCountDesc(
                org.springframework.data.domain.PageRequest.of(0, limit)
        );
    }
}
