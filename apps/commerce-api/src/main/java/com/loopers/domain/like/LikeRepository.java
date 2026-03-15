package com.loopers.domain.like;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LikeRepository {
    Like save(Like like);
    void delete(Like like);
    Optional<Like> findByUserIdAndProductId(String userId, Long productId);
    boolean existsByUserIdAndProductId(String userId, Long productId);
    Long countByProductId(Long productId);
    Map<Long, Long> findLikeCountsByProductIds(List<Long> productIds);
}
