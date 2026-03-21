package com.loopers.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(Long id);
    List<Product> findAllByIds(Collection<Long> ids);
    List<Product> findAllByIdsWithLock(List<Long> ids);
    Optional<Product> findByIdWithLock(Long id);
    Page<Product> findProducts(ProductSearchCondition condition);
    void incrementLikeCount(Long productId);
    void decrementLikeCount(Long productId);
}
