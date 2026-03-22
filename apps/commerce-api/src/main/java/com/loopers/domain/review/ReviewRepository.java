package com.loopers.domain.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository {
    Review save(Review review);
    boolean existsByOrderIdAndProductId(Long orderId, Long productId);
    Page<Review> findByProductId(Long productId, Pageable pageable);
    List<Review> findByProductIdWithCursor(Long productId, Long cursor, int size);
    Optional<Double> getAverageRatingByProductId(Long productId);
    long countByProductId(Long productId);
}
