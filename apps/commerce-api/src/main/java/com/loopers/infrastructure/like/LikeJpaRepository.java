package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LikeJpaRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByUserIdAndProductId(String userId, Long productId);
    boolean existsByUserIdAndProductId(String userId, Long productId);
    Long countByProductId(Long productId);

    @Query("SELECT l.productId as productId, COUNT(l) as likeCount " +
            "FROM Like l WHERE l.productId IN :productIds GROUP BY l.productId")
    List<Map<String, Object>> findLikeCountsByProductIds(@Param("productIds") List<Long> productIds);
}
