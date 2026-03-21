package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LikeJpaRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByUserIdAndProductId(String userId, Long productId);
    boolean existsByUserIdAndProductId(String userId, Long productId);
    Long countByProductId(Long productId);

    @Query("SELECT l.productId as productId, COUNT(l) as likeCount " +
            "FROM Like l WHERE l.productId IN :productIds GROUP BY l.productId")
    List<LikeCountProjection> findLikeCountsByProductIds(@Param("productIds") List<Long> productIds);

    interface LikeCountProjection {
        Long getProductId();
        Long getLikeCount();
    }

    List<Like> findByUserId(String userId);

    @Query("SELECT l.productId FROM Like l WHERE l.userId = :userId AND l.productId IN :productIds")
    List<Long> findProductIdsByUserIdAndProductIds(@Param("userId") String userId, @Param("productIds") List<Long> productIds);
}
