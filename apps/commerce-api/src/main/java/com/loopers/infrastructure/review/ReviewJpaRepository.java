package com.loopers.infrastructure.review;

import com.loopers.domain.review.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewJpaRepository extends JpaRepository<Review, Long> {
    boolean existsByOrder_IdAndProductId(Long orderId, Long productId);
    Page<Review> findByProductId(Long productId, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.productId = :productId")
    Optional<Double> getAverageRatingByProductId(@Param("productId") Long productId);

    long countByProductId(Long productId);
}
