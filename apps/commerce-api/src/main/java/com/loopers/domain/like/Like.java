package com.loopers.domain.like;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Table(name = "likes", uniqueConstraints = @UniqueConstraint(name = "uq_likes_user_product", columnNames = {"user_id", "product_id"}))
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Like extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    private Like(String userId, Long productId) {
        this.userId = userId;
        this.productId = productId;
    }

    public static Like create(String userId, Long productId) {
        return new Like(userId, productId);
    }
}
