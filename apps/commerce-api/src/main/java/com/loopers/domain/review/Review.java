package com.loopers.domain.review;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.order.Order;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(
        name = "uq_reviews_order_product", columnNames = {"order_id", "product_id"}))
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    private Review(Order order, Long productId, String userId, int rating, String content) {
        validateRating(rating);
        validateContent(content);
        this.order = order;
        this.productId = productId;
        this.userId = userId;
        this.rating = rating;
        this.content = content;
    }

    public static Review create(Order order, Long productId, String userId, int rating, String content) {
        return new Review(order, productId, userId, rating, content);
    }

    private void validateRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new CoreException(ErrorType.BAD_REQUEST, "평점은 1~5 사이여야 합니다.");
        }
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "리뷰 내용은 필수입니다.");
        }
        if (content.length() > 500) {
            throw new CoreException(ErrorType.BAD_REQUEST, "리뷰 내용은 500자 이하여야 합니다.");
        }
    }

    public Long getOrderId() {
        return this.order.getId();
    }
}
