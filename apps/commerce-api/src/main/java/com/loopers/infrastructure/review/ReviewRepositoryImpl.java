package com.loopers.infrastructure.review;

import com.loopers.domain.review.Review;
import com.loopers.domain.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReviewRepositoryImpl implements ReviewRepository {

    private final ReviewJpaRepository jpaRepository;

    @Override
    public Review save(Review review) {
        return jpaRepository.save(review);
    }

    @Override
    public boolean existsByOrderIdAndProductId(Long orderId, Long productId) {
        return jpaRepository.existsByOrder_IdAndProductId(orderId, productId);
    }

    @Override
    public Page<Review> findByProductId(Long productId, Pageable pageable) {
        return jpaRepository.findByProductId(productId, pageable);
    }

    @Override
    public Optional<Double> getAverageRatingByProductId(Long productId) {
        return jpaRepository.getAverageRatingByProductId(productId);
    }

    @Override
    public long countByProductId(Long productId) {
        return jpaRepository.countByProductId(productId);
    }
}
