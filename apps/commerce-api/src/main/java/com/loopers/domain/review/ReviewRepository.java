package com.loopers.domain.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ReviewRepository {
    Review save(Review review);
    boolean existsByOrderIdAndProductId(Long orderId, Long productId);
    Page<Review> findByProductId(Long productId, Pageable pageable);
    Optional<Double> getAverageRatingByProductId(Long productId);
    long countByProductId(Long productId);
}
