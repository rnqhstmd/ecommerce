package com.loopers.application.review;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.review.Review;
import com.loopers.domain.review.ReviewService;
import com.loopers.interfaces.api.common.PageResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewFacade {

    private final ReviewService reviewService;
    private final OrderService orderService;

    @Transactional
    public ReviewInfo createReview(String userId, Long orderId, Long productId, int rating, String content) {
        Order order = orderService.getOrderByIdAndUserId(orderId, userId);

        if (order.getStatus() != OrderStatus.PAID) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 완료된 주문에만 리뷰를 작성할 수 있습니다.");
        }

        boolean hasProduct = order.getOrderItems().stream()
                .anyMatch(item -> item.getProductId().equals(productId));
        if (!hasProduct) {
            throw new CoreException(ErrorType.BAD_REQUEST, "해당 주문에 포함되지 않은 상품입니다.");
        }

        Review review = Review.create(order, productId, userId, rating, content);
        Review savedReview = reviewService.createReview(review);
        return ReviewInfo.from(savedReview);
    }

    public ProductReviewInfo getProductReviews(Long productId, Pageable pageable) {
        Page<Review> reviewPage = reviewService.getReviewsByProductId(productId, pageable);
        double averageRating = reviewService.getAverageRating(productId);
        long totalCount = reviewService.getReviewCount(productId);

        double roundedAverage = Math.round(averageRating * 10) / 10.0;

        List<ReviewInfo> reviews = reviewPage.getContent().stream()
                .map(ReviewInfo::from)
                .toList();

        PageResponse<ReviewInfo> pageResponse = PageResponse.of(reviewPage, reviews);

        return new ProductReviewInfo(roundedAverage, totalCount, pageResponse);
    }

    public List<Review> getProductReviewsWithCursor(Long productId, Long cursor, int size) {
        return reviewService.getReviewsByProductIdWithCursor(productId, cursor, size);
    }

    public record ProductReviewInfo(
            double averageRating,
            long totalCount,
            PageResponse<ReviewInfo> reviews
    ) {}
}
