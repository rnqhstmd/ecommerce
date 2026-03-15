package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class LikeRepositoryImpl implements LikeRepository {

    private final LikeJpaRepository jpaRepository;

    @Override
    public Like save(Like like) {
        return jpaRepository.save(like);
    }

    @Override
    public void delete(Like like) {
        jpaRepository.delete(like);
    }

    @Override
    public Optional<Like> findByUserIdAndProductId(String userId, Long productId) {
        return jpaRepository.findByUserIdAndProductId(userId, productId);
    }

    @Override
    public boolean existsByUserIdAndProductId(String userId, Long productId) {
        return jpaRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Override
    public Long countByProductId(Long productId) {
        return jpaRepository.countByProductId(productId);
    }

    @Override
    public Map<Long, Long> findLikeCountsByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return jpaRepository.findLikeCountsByProductIds(productIds).stream()
                .collect(Collectors.toMap(
                        map -> ((Number) map.get("productId")).longValue(),
                        map -> ((Number) map.get("likeCount")).longValue()
                ));
    }
}
