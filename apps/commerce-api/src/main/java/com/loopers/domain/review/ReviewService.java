package com.loopers.domain.review;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;

    @Transactional
    public Review createReview(Review review) {
        if (reviewRepository.existsByOrderIdAndProductId(review.getOrderId(), review.getProductId())) {
            throw new CoreException(ErrorType.CONFLICT, "이미 해당 주문의 상품에 대한 리뷰가 존재합니다.");
        }
        return reviewRepository.save(review);
    }

    public Page<Review> getReviewsByProductId(Long productId, Pageable pageable) {
        return reviewRepository.findByProductId(productId, pageable);
    }

    public double getAverageRating(Long productId) {
        return reviewRepository.getAverageRatingByProductId(productId)
                .orElse(0.0);
    }

    public long getReviewCount(Long productId) {
        return reviewRepository.countByProductId(productId);
    }

    public List<Review> getReviewsByProductIdWithCursor(Long productId, Long cursor, int size) {
        return reviewRepository.findByProductIdWithCursor(productId, cursor, size);
    }
}
